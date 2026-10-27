package com.invitation.gallery;
import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.webkit.*;
import android.view.*;
import java.io.*;
import org.json.*;

public class MainActivity extends Activity {
 WebView web; AppUpdater updater; String pendingId=""; volatile boolean authenticated=false; volatile boolean pickerActive=false;
 final Object authLock=new Object();
 android.content.SharedPreferences auth(){return getSharedPreferences("admin_auth",MODE_PRIVATE);}
 android.content.SharedPreferences session(){return getSharedPreferences("admin_session",MODE_PRIVATE);}
 boolean restoreAdminSession(){return configured()&&session().getBoolean("active",false)&&auth().getString("salt","").equals(session().getString("credentialSalt",""));}
 boolean setAdminSession(boolean active){boolean saved=active?session().edit().putBoolean("active",true).putString("credentialSalt",auth().getString("salt","")).commit():session().edit().clear().commit();authenticated=active&&saved;return saved;}
 boolean configured(){return auth().contains("hash");}
 String hashPassword(String password,byte[] salt)throws Exception{
  javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(password.toCharArray(),salt,120000,256);
  try{return android.util.Base64.encodeToString(javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),android.util.Base64.NO_WRAP);}finally{spec.clearPassword();}
 }
 boolean matches(String password)throws Exception{return java.security.MessageDigest.isEqual(hashPassword(password,android.util.Base64.decode(auth().getString("salt",""),0)).getBytes("UTF-8"),auth().getString("hash","").getBytes("UTF-8"));}
 boolean storePassword(String password)throws Exception{byte[] salt=new byte[32];new java.security.SecureRandom().nextBytes(salt);return auth().edit().putString("salt",android.util.Base64.encodeToString(salt,2)).putString("hash",hashPassword(password,salt)).putInt("failures",0).putLong("lockedUntil",0).commit();}
 boolean saveCatalog(String json){try{new JSONObject(json);File tmp=new File(getFilesDir(),"catalog.tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(json.getBytes("UTF-8"));out.getFD().sync();}return tmp.renameTo(new File(getFilesDir(),"catalog.json"));}catch(Exception e){return false;}}
 @Override public void onCreate(Bundle state){super.onCreate(state);authenticated=restoreAdminSession();
  getWindow().getDecorView().setSystemUiVisibility(5894);
  web=new WebView(this); setContentView(web);updater=new AppUpdater(this); if(android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE == (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)) WebView.setWebContentsDebuggingEnabled(true);
  web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setMediaPlaybackRequiresUserGesture(true);
  web.addJavascriptInterface(new Bridge(),"Native");
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
    Uri u=r.getUrl(); if(!"app.local".equals(u.getHost()))return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
    try {String p=u.getPath(); if(p.startsWith("/media/")){String name=p.substring(7);if(!name.matches("[a-zA-Z0-9.-]+"))throw new IOException(); File f=new File(getFilesDir(),name);return new WebResourceResponse(mimeFor(name),null,new FileInputStream(f));}
     if("/admin.js".equals(p))return new WebResourceResponse("application/javascript","UTF-8",getAssets().open("admin.js"));
     if("/admin-pages.js".equals(p))return new WebResourceResponse("application/javascript","UTF-8",getAssets().open("admin-pages.js"));
     if("/designs.js".equals(p))return new WebResourceResponse("application/javascript","UTF-8",getAssets().open("designs.js"));
     if("/updates.js".equals(p))return new WebResourceResponse("application/javascript","UTF-8",getAssets().open("updates.js"));
     if("/layout.css".equals(p))return new WebResourceResponse("text/css","UTF-8",getAssets().open("layout.css"));
     return new WebResourceResponse("text/html","UTF-8",getAssets().open("index.html"));
    }catch(Exception e){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
   }
  }); web.loadUrl("https://app.local/");
 }
 class Bridge {
  @JavascriptInterface public String updateInfo(){return updater.snapshot();}
  @JavascriptInterface public void checkUpdate(){if(authenticated)updater.check();}
  @JavascriptInterface public void downloadUpdate(){if(authenticated)updater.download();}
  @JavascriptInterface public void cancelUpdate(){if(authenticated)updater.cancel();}
  @JavascriptInterface public void installUpdate(){if(authenticated)runOnUiThread(()->updater.install());}
  @JavascriptInterface public void openUpdateRepository(){runOnUiThread(()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(AppUpdater.REPOSITORY+"/releases")));}catch(Exception e){js("toast('Browser မတွေ့ပါ။')");}});}
  @JavascriptInterface public String authState(){try{return new JSONObject().put("configured",configured()).put("authenticated",authenticated).put("retryAfter",Math.max(0,(auth().getLong("lockedUntil",0)-System.currentTimeMillis()+999)/1000)).toString();}catch(Exception e){return "{}";}}
  @JavascriptInterface public String setupPassword(String password){synchronized(authLock){if(configured())return "exists";if(password.length()<6||password.length()>128)return "weak";try{if(!storePassword(password))return "error";return setAdminSession(true)?"ok":"error";}catch(Exception e){return "error";}}}
  @JavascriptInterface public String login(String password){synchronized(authLock){if(!configured())return "unconfigured";if(auth().getLong("lockedUntil",0)>System.currentTimeMillis())return "locked";try{if(matches(password)){auth().edit().putInt("failures",0).putLong("lockedUntil",0).commit();return setAdminSession(true)?"ok":"error";}int failures=auth().getInt("failures",0)+1;auth().edit().putInt("failures",failures>=5?0:failures).putLong("lockedUntil",failures>=5?System.currentTimeMillis()+30000:0).commit();return failures>=5?"locked":"invalid";}catch(Exception e){return "error";}}}
  @JavascriptInterface public String changePassword(String current,String next){synchronized(authLock){if(!authenticated)return "unauthorized";if(next.length()<6||next.length()>128)return "weak";try{if(!matches(current))return "invalid";if(!storePassword(next))return "error";setAdminSession(false);return "ok";}catch(Exception e){return "error";}}}
  @JavascriptInterface public void logout(){synchronized(authLock){setAdminSession(false);}}
  @JavascriptInterface public boolean initialize(String json){return !configured()&&!new File(getFilesDir(),"catalog.json").exists()&&saveCatalog(json);}
  @JavascriptInterface public String read(){try{return new String(java.nio.file.Files.readAllBytes(new File(getFilesDir(),"catalog.json").toPath()),"UTF-8");}catch(Exception e){return "";}}
  @JavascriptInterface public boolean save(String json){return authenticated&&saveCatalog(json);}
  @JavascriptInterface public void pick(String id){if(!authenticated)return;pendingId=id;runOnUiThread(()->{
   if(!authenticated)return;
   Intent i;
   if(Build.VERSION.SDK_INT>=33){i=new Intent(android.provider.MediaStore.ACTION_PICK_IMAGES);i.putExtra(android.provider.MediaStore.EXTRA_PICK_IMAGES_MAX,Math.min(50,android.provider.MediaStore.getPickImagesMaxLimit()));}
   else{i=new Intent(Intent.ACTION_PICK);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"});i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);}
   try{pickerActive=true;startActivityForResult(i,10);}catch(ActivityNotFoundException e){pickerActive=false;js("toast('Gallery picker မတွေ့ပါ။ Gallery app ကို စစ်ပါ။')");}
  });}
  @JavascriptInterface public void exportData(){if(!authenticated)return;runOnUiThread(()->{if(!authenticated)return;pickerActive=true;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/zip");i.putExtra(Intent.EXTRA_TITLE,"invitation-backup.zip");startActivityForResult(i,20);});}
  @JavascriptInterface public void importData(){if(!authenticated)return;runOnUiThread(()->{if(!authenticated)return;pickerActive=true;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,21);});}
 }
 void js(String script){runOnUiThread(()->web.evaluateJavascript(script,null));}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);pickerActive=false;if(request==31||request==32){updater.publish(request==31&&!getPackageManager().canRequestPackageInstalls()?"permission":"ready",request==31?"Install ကို ထပ်နှိပ်၍ ဆက်လုပ်နိုင်ပါသည်။":"Install မပြီးသေးပါက ပြန်စမ်းနိုင်ပါသည်။");return;}if(result!=RESULT_OK||data==null||!authenticated)return;
  new Thread(()->{try{ Uri selectedUri=data.getData()!=null?data.getData():(data.getClipData()!=null?data.getClipData().getItemAt(0).getUri():null);
   if(request==10){JSONArray a=new JSONArray();int count=data.getClipData()==null?1:data.getClipData().getItemCount();for(int n=0;n<count;n++){Uri u=data.getClipData()==null?data.getData():data.getClipData().getItemAt(n).getUri();String mime=getContentResolver().getType(u);if(mime==null||(!mime.startsWith("image/")&&!mime.startsWith("video/")))continue;boolean video=mime.startsWith("video/");String ext=video?(mime.equals("video/webm")?"webm":mime.equals("video/quicktime")?"mov": "mp4"):mime.equals("image/png")?"png":mime.equals("image/webp")?"webp":mime.equals("image/gif")?"gif":mime.equals("image/heic")?"heic":"jpg";String name=java.util.UUID.randomUUID()+"."+ext;try(InputStream in=getContentResolver().openInputStream(u);OutputStream out=new FileOutputStream(new File(getFilesDir(),name))){copy(in,out);}a.put(new JSONObject().put("src","https://app.local/media/"+name).put("video",video));}js("mediaAdded("+JSONObject.quote(pendingId)+","+a+")");}
   if(request==20){try(java.util.zip.ZipOutputStream z=new java.util.zip.ZipOutputStream(getContentResolver().openOutputStream(selectedUri))){for(File f:getFilesDir().listFiles()){if(!f.getName().equals("catalog.json")&&!f.getName().matches("[a-f0-9-]+\\.(jpg|png|webp|gif|heic|mp4|webm|mov)"))continue;z.putNextEntry(new java.util.zip.ZipEntry(f.getName()));try(InputStream in=new FileInputStream(f)){copy(in,z);}z.closeEntry();}}js("toast('Backup သိမ်းပြီးပါပြီ')");}
   if(request==21){File stage=new File(getCacheDir(),"restore-"+System.currentTimeMillis());stage.mkdirs();try(java.util.zip.ZipInputStream z=new java.util.zip.ZipInputStream(getContentResolver().openInputStream(selectedUri))){java.util.zip.ZipEntry e;while((e=z.getNextEntry())!=null){String name=e.getName();if(!name.equals("catalog.json")&&!name.matches("[a-f0-9-]+\\.(jpg|png|webp|gif|heic|mp4|webm|mov)"))throw new IOException("Invalid backup");try(OutputStream out=new FileOutputStream(new File(stage,name))){copy(z,out);}}}File catalog=new File(stage,"catalog.json");JSONObject obj=new JSONObject(new String(java.nio.file.Files.readAllBytes(catalog.toPath()),"UTF-8"));if(!obj.has("items")||!obj.has("categories"))throw new IOException("Invalid catalog");for(File f:stage.listFiles())if(!f.getName().equals("catalog.json"))java.nio.file.Files.copy(f.toPath(),new File(getFilesDir(),f.getName()).toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);java.nio.file.Files.copy(catalog.toPath(),new File(getFilesDir(),"catalog.json").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);js("location.reload()");}
  }catch(Exception e){android.util.Log.e("InvitationGallery","File operation failed request="+request,e);js("toast('လုပ်ဆောင်မှု မအောင်မြင်ပါ။ ဖိုင်နှင့် storage ကို စစ်ပါ။')");}}).start();
 }
 static String mimeFor(String name){String ext=name.substring(name.lastIndexOf('.')+1);switch(ext){case "mp4":return "video/mp4";case "webm":return "video/webm";case "mov":return "video/quicktime";case "png":return "image/png";case "webp":return "image/webp";case "gif":return "image/gif";case "heic":return "image/heic";default:return "image/jpeg";}}
 static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
 @Override public void onBackPressed(){js("back()");}
}
