package com.fox.facedecter.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jaeger.ninegridimgdemo.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DetectActivity extends AppCompatActivity {

    private TextView responseText;

    public static final int TAKE_PHOTO = 1;

    private ImageView picture;

    private Uri imageUri;

    private String ImagePath = null;
    private String ImageName = null;


    public static final int CHOOSE_PHOTO = 2;

    private Paint paint;
    // 画布
    private Paint textPaint;
    private Canvas canvas;
    // 缩放后的图片
    private Bitmap bitmap;
    // 缩放后的图片副本
    private Bitmap copyBitmap;
    private int mapWidth;
    private int mapHight;

    private String set1;
    private String set2;
    private String set3;

    private int X1 = 0;
    private int X2 = 0;
    private int Y1 = 0;
    private int Y2 = 0;
    private int textSize = 0;
    private String faceAge = null;
    private String faceGender = null;
    private String faceEmotions = null;

    private String message;

    private Switch ageSwitch;
    private Switch genderSwitch;
    private Switch emotionsSwitch;

    private String AgeKey = "false";
    private String GenderKey = "false";
    private String EmotionsKey = "false";
    public static int json_choose=0;

    private Handler handler = null;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);
        picture = (ImageView) findViewById(R.id.picture);

        Button downloadPic = (Button) findViewById(R.id.download_pic) ;
        downloadPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DetectActivity.this,GridStyleActivity.class);
                startActivity(intent);
            }
        });


        handler = new Handler();
        Button sendPost = (Button) findViewById(R.id.send_post);
        sendPost.setOnClickListener(new NoDoubleClickListener() {

            @Override
            protected void onNoDoubleClick(View v) {
                showListDialog();
            }

        });

        Button drawRect = (Button) findViewById(R.id.draw_rect);
        drawRect.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View view) {
                if(ImagePath != null ) {
                    X1 = 0;
                    X2 = 0;
                    Y1 = 0;
                    Y2 = 0;
                    faceGender = null;
                    faceAge = null;
                    faceEmotions = null;
                    showWaitDialog();
                    sendPostWithOkHttp("http://192.168.0.56:8000/v1/detect");
//                    Toast.makeText(DetectActivity.this,"上传中，请等待",Toast.LENGTH_SHORT).show();
                    new Thread() {
                        public void run(){
                            try {
                                Thread.currentThread().sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            handler.post(runnableUi);
                        }
                    }.start();

                }else{
                    Toast.makeText(DetectActivity.this,"请拍照或从相册选择图片",Toast.LENGTH_SHORT).show();
                }

            }
        });

        Button sendGet = (Button) findViewById(R.id.send_get);
        sendGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRequestWithOkHttp("http://192.168.0.248:8000/v0/camera");
            }
        });

        ageSwitch = (Switch) findViewById(R.id.ageSwitch);
        ageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean ageKey) {
               if(ageKey){
                   AgeKey = "true";
                   json_choose = 1;
               }else {
                   AgeKey = "false";
               }
               Log.d("DetectActivity",AgeKey+Integer.toString(json_choose));
            }
        });

        genderSwitch = (Switch) findViewById(R.id.genderSwitch);
        genderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean ganderKey) {
                if(ganderKey){
                    GenderKey = "true";
                }else{
                    GenderKey = "false";
                }
            }
        });

        emotionsSwitch = (Switch) findViewById(R.id.emotionsSwitch);
        emotionsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean emotionsKey) {
                if(emotionsKey){
                    EmotionsKey = "true";
                }else{
                    EmotionsKey = "false";
                }
            }
        });

    }

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.PICK");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "You denied the permission",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    ////////////////////////////////showthepic//////////////////////////////////////////////////////
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        picture.setImageBitmap(bitmap);
                        Log.d("Detectivity", "store in:" + ImagePath+ "\n"
                                + "name: "+ ImageName);
                        copyBitmap = Bitmap.createBitmap(bitmap.getWidth(),
                                bitmap.getHeight(), bitmap.getConfig());
                        // 创建画布
                        canvas = new Canvas(copyBitmap);
                        canvas.drawBitmap(bitmap, new Matrix(), paint);
                        // 将处理后的图片放入imageview中
                        picture.setImageBitmap(copyBitmap);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitKat(data);
                    } else {
                        handleImageBeforeKitKat(data);
                    }
                }
                break;

            default:
                break;
        }
        if(requestCode == 0 && resultCode ==1){
            set1 = data.getStringExtra("set1");
            set2 = data.getStringExtra("set2");
            set3 = data.getStringExtra("set3");
            Toast.makeText(this," Meta is "+set1+"\n Gallary is "+set2+"\n Threholds is "+set3,Toast.LENGTH_SHORT).show();
        }
    }

    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        //String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                ImagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
                ImageName = getImageName(ImagePath);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://download/public_downloads"),
                        Long.valueOf(docId));
                ImagePath = getImagePath(contentUri, null);
                ImageName = getImageName(ImagePath);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            ImagePath = getImagePath(uri, null);
            ImageName = getImageName(ImagePath);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            ImagePath = uri.getPath();
            ImageName = getImageName(ImagePath);
        }
        displayImage();


    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        ImagePath = getImagePath(uri, null);
        ImageName = getImageName(ImagePath);
        displayImage();


    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private String getImageName(String filePath){
        String name = null;
        int start=filePath.lastIndexOf("/");
        int end=filePath.lastIndexOf(".");
        if(start!=-1 && end!=-1){
            name = filePath.substring(start+1);
            return name;
        }else{
            return null;
        }

    }

    private void displayImage() {
        if (ImagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(ImagePath);
            //picture.setImageBitmap(bitmap);
            Log.d("DetectActivity", "get image from:" + ImagePath + "\n" + "name: "+ ImageName);

            copyBitmap = Bitmap.createBitmap(bitmap.getWidth(),
                    bitmap.getHeight(), bitmap.getConfig());
            // 创建画布
            canvas = new Canvas(copyBitmap);
            canvas.drawBitmap(bitmap, new Matrix(), paint);
            // 将处理后的图片放入imageview中
            picture.setImageBitmap(copyBitmap);
            mapWidth = bitmap.getWidth();
            mapHight = bitmap.getHeight();

        } else {
            Toast.makeText(this, "failed to get image",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void takePhoto(){
        X1 = 0;
        X2 = 0;
        Y1 = 0;
        Y2 = 0;
        faceGender = null;
        faceAge = null;
        faceEmotions = null;
        File outputImage = new File(getExternalCacheDir(),
                "output_image.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= 24) {
            imageUri = FileProvider.getUriForFile(DetectActivity.this,
                    "com.example.cameraalbumtest.fileprovider", outputImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
        }
        ImagePath = imageUri.toString();
        ImageName = getImageName(ImagePath);
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }

    public void choosePhoto(){
        X1 = 0;
        X2 = 0;
        Y1 = 0;
        Y2 = 0;
        faceGender = null;
        faceAge = null;
        faceEmotions = null;
        if (ContextCompat.checkSelfPermission(DetectActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DetectActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            openAlbum();
        }
    }

    private void showListDialog(){
        final String[] items = {"拍照","相册"};
        AlertDialog.Builder listDialog = new AlertDialog.Builder(DetectActivity.this);

        listDialog.setTitle("请选择图片");
        listDialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i == 0){
                    takePhoto();
//                    Toast.makeText(DetectActivity.this,"click 1",Toast.LENGTH_SHORT).show();
                }
                if(i == 1){
                    choosePhoto();
//                    Toast.makeText(DetectActivity.this,"click 2",Toast.LENGTH_SHORT).show();
                }
            }
        });
        listDialog.show();
    }

    private void showWaitDialog(){
        //等待Dialog具有屏蔽其他控件的交互能力
        //@setCancelable 为使屏幕不可点击，设置为不可取消(false)
        //下载等事件完成后，主动调用函数关闭该Dialog
        final ProgressDialog waitingDialog = new ProgressDialog(DetectActivity.this);
        waitingDialog.setTitle("识别人脸中");
        waitingDialog.setMessage("请等待……");
        waitingDialog.setIndeterminate(true);
        waitingDialog.setCancelable(false);
        waitingDialog.show();
        new Thread() {
            public void run(){
                try {
                    Thread.currentThread().sleep(4500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                waitingDialog.dismiss();
            }
        }.start();
    }

    public abstract class NoDoubleClickListener implements View.OnClickListener {
        public static final int MIN_CLICK_DELAY_TIME = 1000;
        private long lastClickTime = 0;

        @Override
        public void onClick(View v) {
            long currentTime = Calendar.getInstance().getTimeInMillis();
            if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                lastClickTime = currentTime;
                onNoDoubleClick(v);
            }
        }

        protected abstract void onNoDoubleClick(View v);

    }


    public static class HttpUtil {
        public static void sendOkHttpRequest(String address, okhttp3.Callback callback) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(address)
                    .addHeader("Host", "127.0.0.1")
                    .addHeader("Authorization", "Token LVHc-HkZS")
                    .get()
                    .build();
            client.newCall(request).enqueue(callback);
        }

        public static void postOkHttpRequest(String address,String filePath,String fileName,String AgeKey,okhttp3.Callback callback) {


            OkHttpClient client = new OkHttpClient();
            File file = new File(filePath);
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/png"), file);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("photo", fileName, fileBody)
                    .addFormDataPart("emotions","true")
                    .addFormDataPart("gender","true")
                    .addFormDataPart("age",AgeKey)
                    .build();

            Request requestPostFile = new Request.Builder()
                    .url(address)
                    .addHeader("Host", "127.0.0.1")
                    .addHeader("Authorization", "Token LVHc-HkZS")
                    .post(requestBody)
                    .build();

            client.newCall(requestPostFile).enqueue(callback);

        }
    }



    private void sendPostWithOkHttp(final String address){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUtil.postOkHttpRequest(address,ImagePath,ImageName,AgeKey,new okhttp3.Callback(){

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //解析JSON
                        String responseData = response.body().string();
                        parseJSONWithJSONObjectOfDetect(responseData);
                    }

                    @Override
                    public void onFailure(Call call, IOException e) {
                    }
                });
            }
        }).start();
    }


    private void sendRequestWithOkHttp(final String address) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUtil.sendOkHttpRequest(address, new okhttp3.Callback() {

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseData = response.body().string();
                        parseJSONWithJSONObjectOfCamera(responseData);
                    }

                    @Override
                    public void onFailure(Call call, IOException e) {
                    }
                });
            }
        }).start();

    }

    private Handler messageHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            Toast.makeText(DetectActivity.this,message,Toast.LENGTH_SHORT).show();
        }
    };

    Runnable runnableUi = new Runnable() {
        @Override
        public void run() {
            if(X1 != 0 && X2 != 0) {
                //创建画笔
                paint = new Paint();
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                canvas.drawPaint(paint);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                displayImage();
                //设置为线条模式
                paint.setStyle(Paint.Style.STROKE);
                // 设置画笔颜色
                paint.setColor(Color.GREEN);
                // 设置画笔宽度
                paint.setStrokeWidth(4);
                //画框
                canvas.drawRect(X1, Y1, X2, Y2, paint);

                Y2 = Y2+4;
                textSize = 10+mapHight*mapWidth/20000;
                textPaint = new Paint();
                textPaint.setColor(Color.RED);
                textPaint.setStyle(Paint.Style.FILL);
                textPaint.setTextSize(textSize);
                Typeface font;
                font = Typeface.create(Typeface.DEFAULT,Typeface.NORMAL);
                paint.setTypeface(font);
                if(AgeKey == "true") {
                    canvas.drawText("年龄: " + faceAge + " 岁", X1 - 2 * textSize, Y2 + textSize, textPaint);
                    if(GenderKey == "true"){
                        canvas.drawText("性别: "+faceGender, X1 -2*textSize, Y2 + 2*textSize , textPaint);
                        if(EmotionsKey == "true"){
                            canvas.drawText("情绪: " + faceEmotions, X1 - 2 * textSize, Y2 + 3*textSize, textPaint);
                        }
                    }else{
                        if(EmotionsKey == "true"){
                            canvas.drawText("情绪: " + faceEmotions, X1 - 2 * textSize, Y2 + 2*textSize, textPaint);
                        }
                    }
                }else {
                    if (GenderKey == "true") {
                        canvas.drawText("性别: " + faceGender, X1 - 2*textSize, Y2 + textSize, textPaint);
                        if (EmotionsKey == "true") {
                            canvas.drawText("情绪: " + faceEmotions, X1 - 2 * textSize, Y2 + 2* textSize, textPaint);
                        }
                    } else {
                        if (EmotionsKey == "true") {
                            canvas.drawText("情绪: " + faceEmotions, X1 - 2*textSize, Y2 + textSize, textPaint);
                        }
                    }
                }

                Log.d("DetectActivity",Integer.toString(mapWidth*mapHight));
                Log.d("DetectActivity",Integer.toString(textSize));
                // 刷新image
                picture.invalidate();

            }else{
                message = "识别失败，没有人脸";
            }messageHandler.sendMessage((messageHandler.obtainMessage()));

        }
    };




    //解包不同的JSON
    public class Camera{
        private String meta;
        private String url;
        private String id;

        public String getMeta(){
            return meta;
        }
        public void setMeta(String meta){
            this.meta = meta;
        }

        public String getUrl(){
            return url;
        }
        public void setUrl(String url){
            this.url = url;
        }

        public String getId(){
            return id;
        }
        public void setId(String id){
            this.id = id;
        }

    }
    private void parseJSONWithJSONObjectOfCamera(String jsonData) {
        Gson gson = new Gson();
        List<Camera> cameraList = gson.fromJson(jsonData, new TypeToken<List<Camera>>() {
        }.getType());
        for (Camera camera : cameraList) {
            Log.d("DetectActivity", "meta is " + camera.getMeta());
            Log.d("DetectActivity", "url is " + camera.getUrl());
            Log.d("DetectActivity", "id is " + camera.getId());
        }
    }


    private void parseJSONWithJSONObjectOfDetect(String jsonData)
    {
        try {
            JSONObject jsonObject1=new JSONObject(jsonData);
            JSONArray jsonArray = jsonObject1.optJSONArray("faces");
            if(jsonArray.length()>0){
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);

                String x1 = jsonObject.getString("x1");
                String x2 = jsonObject.getString("x2");
                String y1 = jsonObject.getString("y1");
                String y2 = jsonObject.getString("y2");
                X1 = Integer.parseInt(x1);
                X2 = Integer.parseInt(x2);
                Y1 = Integer.parseInt(y1);
                Y2 = Integer.parseInt(y2);
                Log.d("DetectActivity", "x1 is " + x1);
                Log.d("DetectActivity", "x2 is " + x2);
                Log.d("DetectActivity", "y1 is " + y1);
                Log.d("DetectActivity", "y2 is " + y2);

                if(AgeKey == "true") {
                    String age = jsonObject.getString("age");
                    faceAge = age.substring(0,2);
                    Log.d("DetectActivity","age is "+age);
                }
                if(GenderKey == "true") {
                    String gender = jsonObject.getString("gender");
                    faceGender = gender;
                    Log.d("DetectActivity","gender is "+gender);
                }
                if(EmotionsKey == "true") {
                    String emotions = jsonObject.getString("emotions");
                    faceEmotions = emotions;
                    Log.d("DetectActivity", "emotions is " + emotions);
                }
                message = "识别成功";
            }
            }else{

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            message = "上传失败";
        }


    }
}



