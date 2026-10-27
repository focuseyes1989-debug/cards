package com.invitation.gallery;

import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

final class AppUpdater {
 static final String REPOSITORY="https://github.com/focuseyes1989-debug/cards";
 static final String MANIFEST=REPOSITORY+"/releases/latest/download/update.json";
 static final long MAX_APK=250L*1024*1024;
 final MainActivity activity;
 final File directory,apk;
 volatile boolean busy=false,cancelled=false;
 JSONObject state=new JSONObject(),release;
 AppUpdater(MainActivity activity){this.activity=activity;directory=new File(activity.getCacheDir(),"updates");directory.mkdirs();apk=new File(directory,"update.apk");publish("idle","");}
 long installedCode()throws Exception{PackageInfo info=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);return code(info);}
 static long code(PackageInfo p){return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
 synchronized String snapshot(){return state.toString();}
 synchronized void publish(String status,String message){
  try {state=new JSONObject().put("status",status).put("message",message).put("repository",REPOSITORY);
   PackageInfo current=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);
   state.put("currentVersion",current.versionName).put("currentCode",code(current));
   if(release!=null)state.put("versionName",release.optString("versionName")).put("versionCode",release.optLong("versionCode")).put("notes",release.optString("notes")).put("size",release.optLong("size"));
   notifyPage();
  }catch(Exception e){android.util.Log.e("InvitationUpdater","Update state",e);}
 }
 synchronized void progress(long bytes,long size){try{state.put("downloaded",bytes).put("size",size);notifyPage();}catch(Exception ignored){}}
 void notifyPage(){activity.js("if(typeof updateState==='function')updateState("+state+")");}
 static boolean allowed(URL url){String h=url.getHost().toLowerCase(Locale.ROOT);return "https".equals(url.getProtocol())&&url.getUserInfo()==null&&(h.equals("github.com")||h.equals("release-assets.githubusercontent.com")||h.equals("objects.githubusercontent.com"));}
 HttpURLConnection connect(String address)throws Exception{
  URL url=new URL(address);
  for(int n=0;n<6;n++){
   if(!allowed(url))throw new IOException("Unsupported download host");
   HttpURLConnection connection=(HttpURLConnection)url.openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(25000);connection.setRequestProperty("User-Agent","Invitation-Gallery-Android");connection.setRequestProperty("Accept-Encoding","identity");
   int status=connection.getResponseCode();
   if(status>=300&&status<=399){String location=connection.getHeaderField("Location");connection.disconnect();if(location==null)throw new IOException("Missing redirect");url=new URL(url,location);continue;}
   if(status==404){connection.disconnect();throw new FileNotFoundException("Release not found");}
   if(status!=200){connection.disconnect();throw new IOException("HTTP "+status);}
   return connection;
  }throw new IOException("Too many redirects");
 }
 synchronized void check(){
  if(busy||!activity.authenticated)return;busy=true;cancelled=false;release=null;publish("checking","Update စစ်နေပါသည်…");
  new Thread(()->{try{
   HttpURLConnection connection=connect(MANIFEST);String json;
   try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>65536)throw new IOException("Manifest too large");out.write(b,0,n);}json=out.toString("UTF-8");}finally{connection.disconnect();}
   JSONObject candidate=new JSONObject(json);validateManifest(candidate);release=candidate;
   if(candidate.getLong("versionCode")<=installedCode()){publish("current","နောက်ဆုံး version ကို သုံးနေပါပြီ။");}
   else if(candidate.optInt("minSdk",26)>Build.VERSION.SDK_INT){publish("unsupported","Version အသစ်သည် ဤ Android version ကို မထောက်ပံ့ပါ။");}
   else publish("available","Version အသစ် ရရှိနိုင်ပါပြီ။");
  }catch(FileNotFoundException e){publish("no_release","GitHub မှာ update release မတင်ရသေးပါ။");}
  catch(Exception e){android.util.Log.e("InvitationUpdater","Check failed",e);publish("error","Update စစ်မရပါ။ Internet connection ကို စစ်ပြီး ပြန်စမ်းပါ။");}
  finally{busy=false;}},"invitation-update-check").start();
 }
 static void validateManifest(JSONObject m)throws Exception{
  if(m.optInt("schema")!=1||!"com.invitation.gallery".equals(m.optString("packageName"))||m.getLong("versionCode")<1||m.getString("versionName").length()>40||m.getString("versionName").isEmpty())throw new IOException("Invalid version metadata");
  String address=m.getString("apkUrl");if(!address.startsWith(REPOSITORY+"/releases/download/")||!address.endsWith("/Invitation-Gallery.apk"))throw new IOException("Invalid APK source");
  if(!m.getString("sha256").matches("[a-fA-F0-9]{64}")||m.getLong("size")<=0||m.getLong("size")>MAX_APK||m.optString("notes").length()>6000)throw new IOException("Invalid APK metadata");
 }
 synchronized void download(){
  if(busy||release==null||!activity.authenticated)return;
  final JSONObject expected=release;
  try{if(expected.getLong("versionCode")<=installedCode())return;}catch(Exception e){return;}
  busy=true;cancelled=false;publish("downloading","APK အသစ် download လုပ်နေပါသည်…");
  new Thread(()->{File part=new File(directory,"update.part");try{
   HttpURLConnection connection=connect(expected.getString("apkUrl"));long size=expected.getLong("size"),bytes=0,last=0;
   try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(part)){
    byte[] buffer=new byte[65536];int n;
    while((n=in.read(buffer))!=-1){if(cancelled)throw new InterruptedIOException("Cancelled");bytes+=n;if(bytes>size||bytes>MAX_APK)throw new IOException("Unexpected download size");out.write(buffer,0,n);long now=System.currentTimeMillis();if(now-last>200){progress(bytes,size);last=now;}}
    out.getFD().sync();
   }finally{connection.disconnect();}
   if(cancelled)throw new InterruptedIOException("Cancelled");
   validateApk(part,expected);java.nio.file.Files.move(part.toPath(),apk.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);progress(size,size);publish("ready","APK စစ်ဆေးပြီးပါပြီ။ Install လုပ်နိုင်ပါပြီ။");
  }catch(InterruptedIOException e){part.delete();publish(cancelled?"cancelled":"error",cancelled?"Download ရပ်လိုက်ပါပြီ။":"Connection အချိန်ကျော်သွားပါသည်။ ပြန်စမ်းပါ။");}
  catch(Exception e){part.delete();android.util.Log.e("InvitationUpdater","Download failed",e);publish("error","Download သို့မဟုတ် APK စစ်ဆေးမှု မအောင်မြင်ပါ။ ပြန်စမ်းပါ။");}
  finally{busy=false;}},"invitation-update-download").start();
 }
 void validateApk(File file,JSONObject expected)throws Exception{
  if(file.length()!=expected.getLong("size"))throw new IOException("Incomplete APK");
  MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}
  StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));if(!hex.toString().equalsIgnoreCase(expected.getString("sha256")))throw new IOException("APK checksum mismatch");
  PackageManager pm=activity.getPackageManager();PackageInfo candidate=pm.getPackageArchiveInfo(file.getAbsolutePath(),PackageManager.GET_SIGNATURES),current=pm.getPackageInfo(activity.getPackageName(),PackageManager.GET_SIGNATURES);
  if(candidate==null||!activity.getPackageName().equals(candidate.packageName)||code(candidate)!=expected.getLong("versionCode")||code(candidate)<=code(current))throw new IOException("APK package/version mismatch");
  if(candidate.signatures==null||current.signatures==null||candidate.signatures.length!=current.signatures.length)throw new IOException("APK signature missing");
  Set<String> oldSignatures=new HashSet<>(),newSignatures=new HashSet<>();for(Signature s:current.signatures)oldSignatures.add(s.toCharsString());for(Signature s:candidate.signatures)newSignatures.add(s.toCharsString());if(!oldSignatures.equals(newSignatures))throw new IOException("APK signing identity mismatch");
 }
 void cancel(){cancelled=true;}
 synchronized void install(){
  if(busy||!activity.authenticated||release==null||!apk.exists())return;
  busy=true;publish("verifying","APK ကို install မလုပ်မီ ပြန်စစ်နေပါသည်…");
  final JSONObject expected=release;
  new Thread(()->{try{validateApk(apk,expected);activity.runOnUiThread(()->{busy=false;if(activity.authenticated)launchInstaller();else publish("ready","Admin ပြန်ဝင်ပြီး Install နှိပ်ပါ။");});}
   catch(Exception e){busy=false;android.util.Log.e("InvitationUpdater","Verification failed",e);publish("error","APK စစ်ဆေးမှု မအောင်မြင်ပါ။ Update ကို download ပြန်လုပ်ပါ။");}},"invitation-update-verify").start();
 }
 void launchInstaller(){
  try{
   if(!activity.getPackageManager().canRequestPackageInstalls()){
    publish("permission","ဤ app အတွက် Allow from this source ဖွင့်ပြီး ပြန်လာပါ။");activity.pickerActive=true;
    activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())),31);return;
   }
   Intent intent=new Intent(Intent.ACTION_VIEW);intent.setDataAndType(Uri.parse("content://"+activity.getPackageName()+".updates/apk"),"application/vnd.android.package-archive");intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.putExtra(Intent.EXTRA_RETURN_RESULT,true);activity.pickerActive=true;activity.startActivityForResult(intent,32);publish("ready","Android installer မှာ Update ကို နှိပ်ပါ။");
  }catch(Exception e){activity.pickerActive=false;android.util.Log.e("InvitationUpdater","Install failed",e);publish("error","Installer ဖွင့်မရပါ။ Update ကို ပြန်စစ်ပြီး download ပြန်လုပ်ပါ။");}
 }
}
