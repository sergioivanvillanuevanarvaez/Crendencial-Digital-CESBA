package com.example.credencialdigitalcesba;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.File;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    ScrollView scroll;
    LinearLayout home, form, credencial, cardCredencial, cardReverso;
    FrameLayout flipContainer;
    EditText etNombre, etMatricula, etSemestre, etGrupo, etCorreo;
    Spinner spCarrera, spTipo;
    ImageView imgFoto, imgCredencial, imgQR;
    TextView tvNombre, tvCarrera, tvMatricula, tvSemestre, tvGrupo, tvCorreo, tvTipo;
    Button btnFoto;
    Bitmap foto, qrBitmap;
    String payload;
    File fotoFile;
    boolean frenteVisible = true, girando = false;
    OnBackPressedCallback atrasCallback;
    static final int FOTO = 101, CAMERA = 102, TXT = 103;

    String[] carreras = {"Selecciona tu carrera", "Administración", "Arquitectura", "Comercio Internacional", "Contaduría Pública", "Contaduría y Finanzas", "Criminología", "Derecho", "Diseño y Comunicación Audiovisual", "Enfermería", "Fisioterapia", "Ingeniería Industrial", "Mercadotecnia y Negocios", "Nutrición", "Pedagogía", "Psicología", "Ingeniería en Sistemas"};
    String[] tipos = {"Alumno", "Docente", "Administrativo"};
    int violeta = Color.rgb(108,43,217);
    int navy = Color.rgb(14,34,71);

    @Override protected void onCreate(Bundle b) { super.onCreate(b); setContentView(R.layout.activity_main); bind(); setup(); }

    void bind(){
        scroll=findViewById(R.id.scroll);
        home=findViewById(R.id.home); form=findViewById(R.id.form); credencial=findViewById(R.id.credencial); cardCredencial=findViewById(R.id.cardCredencial); cardReverso=findViewById(R.id.cardReverso); flipContainer=findViewById(R.id.flipContainer);
        etNombre=findViewById(R.id.etNombre); etMatricula=findViewById(R.id.etMatricula); etSemestre=findViewById(R.id.etSemestre); etGrupo=findViewById(R.id.etGrupo); etCorreo=findViewById(R.id.etCorreo);
        spCarrera=findViewById(R.id.spCarrera); spTipo=findViewById(R.id.spTipo); imgFoto=findViewById(R.id.imgFoto); imgCredencial=findViewById(R.id.imgCredencial); imgQR=findViewById(R.id.imgQR);
        tvNombre=findViewById(R.id.tvNombre); tvCarrera=findViewById(R.id.tvCarrera); tvMatricula=findViewById(R.id.tvMatricula); tvSemestre=findViewById(R.id.tvSemestre); tvGrupo=findViewById(R.id.tvGrupo); tvCorreo=findViewById(R.id.tvCorreo); tvTipo=findViewById(R.id.tvTipo);
        btnFoto=findViewById(R.id.btnFoto);
    }

    int dp(float v){ return Math.round(v*getResources().getDisplayMetrics().density); }

    void animarInicio(){
        View prev=findViewById(R.id.homePreview);
        prev.setAlpha(0f); prev.setTranslationY(dp(40));
        prev.animate().alpha(1f).translationY(0f).setStartDelay(250).setDuration(600).setInterpolator(new DecelerateInterpolator()).start();
    }
    void fotoPlaceholder(){
        imgFoto.setBackgroundResource(R.drawable.photo_placeholder);
        imgFoto.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imgFoto.setPadding(dp(34),dp(34),dp(34),dp(34));
        imgFoto.setImageResource(R.drawable.ic_camera);
    }
    void fotoListo(){
        imgFoto.setBackgroundResource(R.drawable.photo_ring);
        imgFoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgFoto.setPadding(dp(3),dp(3),dp(3),dp(3));
        imgFoto.setImageBitmap(foto);
    }

    void circular(View v){ v.setOutlineProvider(ViewOutlineProvider.BACKGROUND); v.setClipToOutline(true); }

    void setup(){
        circular(imgFoto); circular(imgCredencial);
        fotoPlaceholder(); animarInicio();
        float d=getResources().getDisplayMetrics().density*8000f;
        cardCredencial.setCameraDistance(d); cardReverso.setCameraDistance(d);
        spCarrera.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, carreras));
        spTipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tipos));

        atrasCallback=new OnBackPressedCallback(false){ @Override public void handleOnBackPressed(){ atras(); } };
        getOnBackPressedDispatcher().addCallback(this, atrasCallback);

        findViewById(R.id.btnCrear).setOnClickListener(v->showForm());
        btnFoto.setOnClickListener(v->tomarFoto());
        findViewById(R.id.btnGenerar).setOnClickListener(v->generar());
        findViewById(R.id.btnNueva).setOnClickListener(v->{limpiarFormulario(); showForm();});
        findViewById(R.id.btnValidar).setOnClickListener(v->validar());
        findViewById(R.id.btnVoltear).setOnClickListener(v->voltear());
        findViewById(R.id.btnTxt).setOnClickListener(v->guardarTxt());
        flipContainer.setOnClickListener(v->voltear());
        findViewById(R.id.btnAtrasForm).setOnClickListener(v->atras());
        findViewById(R.id.btnAtrasCred).setOnClickListener(v->atras());
    }

    // ---------- Navegación ----------
    void showHome(){ form.setVisibility(View.GONE); credencial.setVisibility(View.GONE); home.setVisibility(View.VISIBLE); atrasCallback.setEnabled(false); scroll.scrollTo(0,0); }
    void showForm(){ home.setVisibility(View.GONE); credencial.setVisibility(View.GONE); form.setVisibility(View.VISIBLE); atrasCallback.setEnabled(true); scroll.scrollTo(0,0); }
    void showCredencial(){ home.setVisibility(View.GONE); form.setVisibility(View.GONE); credencial.setVisibility(View.VISIBLE); atrasCallback.setEnabled(true); scroll.scrollTo(0,0); }
    void atras(){
        if(credencial.getVisibility()==View.VISIBLE) showForm();
        else if(form.getVisibility()==View.VISIBLE) showHome();
    }
    void limpiarFormulario(){
        etNombre.setText(""); etMatricula.setText(""); etSemestre.setText(""); etGrupo.setText(""); etCorreo.setText("");
        spCarrera.setSelection(0); spTipo.setSelection(0);
        foto=null; fotoPlaceholder(); btnFoto.setText("Tomar foto");
    }

    // ---------- Cámara ----------
    void tomarFoto(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){ ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},CAMERA); return; }
        abrirCamara();
    }
    @Override public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] results){
        super.onRequestPermissionsResult(req, perms, results);
        if(req==CAMERA){
            if(results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) abrirCamara();
            else Toast.makeText(this,"Permite el acceso a la cámara para tomar tu foto",Toast.LENGTH_LONG).show();
        }
    }
    void abrirCamara(){
        try{
            fotoFile=new File(getCacheDir(),"foto_credencial.jpg");
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",fotoFile);
            Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT,uri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i,FOTO);
        }catch(Exception e){ Toast.makeText(this,"No se pudo abrir la cámara",Toast.LENGTH_SHORT).show(); }
    }
    @Override protected void onActivityResult(int req,int result,@Nullable Intent data){
        super.onActivityResult(req,result,data);
        if(req==TXT){
            if(result==Activity.RESULT_OK && data!=null && data.getData()!=null) escribirTxt(data.getData());
            return;
        }
        if(req==FOTO && result==Activity.RESULT_OK && fotoFile!=null && fotoFile.exists()){
            Bitmap bmp=cargarFoto(fotoFile);
            if(bmp!=null){ foto=bmp; fotoListo(); btnFoto.setText("Volver a tomar foto"); }
        }
    }
    Bitmap cargarFoto(File f){
        try{
            BitmapFactory.Options o=new BitmapFactory.Options(); o.inJustDecodeBounds=true; BitmapFactory.decodeFile(f.getAbsolutePath(),o);
            int lado=Math.max(o.outWidth,o.outHeight), muestra=1; while(lado/muestra>1000) muestra*=2;
            BitmapFactory.Options o2=new BitmapFactory.Options(); o2.inSampleSize=muestra;
            Bitmap bmp=BitmapFactory.decodeFile(f.getAbsolutePath(),o2);
            if(bmp==null) return null;
            int orient=new ExifInterface(f.getAbsolutePath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
            int grados=orient==ExifInterface.ORIENTATION_ROTATE_90?90:orient==ExifInterface.ORIENTATION_ROTATE_180?180:orient==ExifInterface.ORIENTATION_ROTATE_270?270:0;
            if(grados!=0){ Matrix m=new Matrix(); m.postRotate(grados); bmp=Bitmap.createBitmap(bmp,0,0,bmp.getWidth(),bmp.getHeight(),m,true); }
            return bmp;
        }catch(Exception e){ return null; }
    }

    // ---------- Generar credencial ----------
    TextInputLayout til(EditText et){
        ViewParent p=et.getParent();
        while(p!=null && !(p instanceof TextInputLayout)) p=p.getParent();
        return (TextInputLayout)p;
    }
    void limpiarErrores(){
        for(EditText e: new EditText[]{etNombre,etMatricula,etSemestre,etGrupo,etCorreo}){ TextInputLayout t=til(e); if(t!=null) t.setError(null); }
    }
    boolean error(EditText et, String msg){
        TextInputLayout t=til(et);
        if(t!=null) t.setError(msg); else et.setError(msg);
        et.requestFocus(); return false;
    }

    boolean datosValidos(String nombre,String matricula,String sem,String grupo,String correo,int c){
        if(nombre.split("\\s+").length<2 || !nombre.matches("[\\p{L} .'-]+")) return error(etNombre,"Escribe tu nombre completo");
        if(!matricula.matches("[A-Za-z0-9-]{4,20}")) return error(etMatricula,"Matrícula no válida (mínimo 4 caracteres)");
        if(c==0){ Toast.makeText(this,"Selecciona tu carrera",Toast.LENGTH_SHORT).show(); return false; }
        int n; try{ n=Integer.parseInt(sem); }catch(NumberFormatException e){ n=0; }
        if(n<1||n>12) return error(etSemestre,"Escribe un número del 1 al 12");
        if(grupo.isEmpty()||grupo.contains("|")||grupo.contains("=")) return error(etGrupo,"Escribe tu grupo");
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) return error(etCorreo,"Correo no válido");
        if(foto==null){ Toast.makeText(this,"Toma tu foto para generar la credencial",Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }

    void generar(){
        String nombre=etNombre.getText().toString().trim().replaceAll("\\s+"," "), matricula=etMatricula.getText().toString().trim(), sem=etSemestre.getText().toString().trim(), grupo=etGrupo.getText().toString().trim(), correo=etCorreo.getText().toString().trim();
        int c=spCarrera.getSelectedItemPosition();
        limpiarErrores();
        if(!datosValidos(nombre,matricula,sem,grupo,correo,c)) return;
        sem=String.valueOf(Integer.parseInt(sem));
        String carrera=carreras[c]; int color=colorCarrera(carrera); String tipo=spTipo.getSelectedItem().toString();
        tvNombre.setText(nombre); tvCarrera.setText(carrera); tvMatricula.setText(matricula); tvSemestre.setText(sem); tvGrupo.setText(grupo); tvCorreo.setText(correo); tvTipo.setText(tipo);
        cardCredencial.setBackground(gradient(color)); cardReverso.setBackground(gradient(color));
        imgCredencial.setImageBitmap(foto);
        payload="CESBA|NOMBRE="+nombre+"|MATRICULA="+matricula+"|CARRERA="+carrera+"|SEMESTRE="+sem+"|GRUPO="+grupo+"|CORREO="+correo+"|TIPO="+tipo;
        qrBitmap=qr(payload);
        android.graphics.drawable.BitmapDrawable qd=new android.graphics.drawable.BitmapDrawable(getResources(),qrBitmap); qd.setFilterBitmap(false); imgQR.setImageDrawable(qd);

        frenteVisible=true; girando=false;
        cardCredencial.setVisibility(View.VISIBLE); cardCredencial.setRotationY(0f); cardReverso.setVisibility(View.INVISIBLE); cardReverso.setRotationY(0f);
        showCredencial();
        flipContainer.setAlpha(0f); flipContainer.setTranslationY(80f);
        flipContainer.animate().alpha(1f).translationY(0f).setDuration(450).setInterpolator(new DecelerateInterpolator()).start();
        Toast.makeText(this,"Credencial generada correctamente",Toast.LENGTH_SHORT).show();
    }

    void voltear(){
        if(girando) return; girando=true;
        final View sale=frenteVisible?cardCredencial:cardReverso, entra=frenteVisible?cardReverso:cardCredencial;
        frenteVisible=!frenteVisible;
        sale.animate().rotationY(90f).setDuration(180).withEndAction(()->{
            sale.setVisibility(View.INVISIBLE); sale.setRotationY(0f);
            entra.setRotationY(-90f); entra.setVisibility(View.VISIBLE);
            entra.animate().rotationY(0f).setDuration(180).withEndAction(()->girando=false).start();
        }).start();
    }

    android.graphics.drawable.Drawable gradient(int color){
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{navy,ColorUtils.blendARGB(navy,color,0.4f),color});
        g.setCornerRadius(64);
        android.graphics.drawable.GradientDrawable aro1=aro(70), aro2=aro(45);
        android.graphics.drawable.LayerDrawable ld=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{g,aro1,aro2});
        ld.setLayerSize(1,dp(190),dp(190)); ld.setLayerGravity(1,Gravity.END|Gravity.BOTTOM); ld.setLayerInset(1,0,0,dp(18),dp(18));
        ld.setLayerSize(2,dp(130),dp(130)); ld.setLayerGravity(2,Gravity.END|Gravity.BOTTOM); ld.setLayerInset(2,0,0,dp(48),dp(48));
        return ld;
    }
    android.graphics.drawable.GradientDrawable aro(int alpha){
        android.graphics.drawable.GradientDrawable o=new android.graphics.drawable.GradientDrawable();
        o.setShape(android.graphics.drawable.GradientDrawable.OVAL); o.setStroke(dp(1.5f),Color.argb(alpha,255,255,255)); return o;
    }
    int colorCarrera(String c){
        switch(c){case "Administración":return Color.rgb(242,140,40);case "Arquitectura":return Color.rgb(128,128,128);case "Comercio Internacional":return Color.rgb(214,40,40);case "Contaduría Pública":return Color.rgb(185,28,28);case "Contaduría y Finanzas":return Color.rgb(232,93,158);case "Criminología":return Color.rgb(244,196,48);case "Derecho":return Color.rgb(229,161,26);case "Diseño y Comunicación Audiovisual":return Color.rgb(247,168,196);case "Enfermería":return Color.rgb(125,188,232);case "Fisioterapia":return Color.rgb(23,105,170);case "Ingeniería Industrial":return Color.rgb(53,168,83);case "Mercadotecnia y Negocios":return Color.rgb(193,18,31);case "Nutrición":return Color.rgb(242,140,40);case "Pedagogía":return Color.rgb(111,168,220);case "Psicología":return Color.rgb(121,85,72);case "Ingeniería en Sistemas":return Color.rgb(123,44,191);default:return violeta;}}

    // ---------- QR ----------
    Bitmap qr(String text){
        try{
            Map<com.google.zxing.EncodeHintType,Object> hints=new HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET,"UTF-8");
            hints.put(com.google.zxing.EncodeHintType.ERROR_CORRECTION,com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L);
            hints.put(com.google.zxing.EncodeHintType.MARGIN,4);
            BitMatrix m=new QRCodeWriter().encode(text,BarcodeFormat.QR_CODE,0,0,hints);   // tamaño natural: 1 módulo = 1 celda
            int n=m.getWidth(), escala=Math.max(2, dp(220)/n), size=n*escala;                 // escala entera = módulos nítidos
            Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
            android.graphics.Canvas cv=new android.graphics.Canvas(b); cv.drawColor(Color.WHITE);
            android.graphics.Paint pt=new android.graphics.Paint(); pt.setColor(Color.BLACK);
            for(int y=0;y<n;y++) for(int x=0;x<n;x++) if(m.get(x,y)) cv.drawRect(x*escala,y*escala,(x+1)*escala,(y+1)*escala,pt);
            return b;
        }catch(WriterException e){ return null; }
    }

    String leerQR(Bitmap b){
        try{
            int w=b.getWidth(), h=b.getHeight(); int[] px=new int[w*h]; b.getPixels(px,0,w,0,0,w,h);
            BinaryBitmap bb=new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(w,h,px)));
            return new QRCodeReader().decode(bb).getText();
        }catch(Exception e){ return null; }
    }

    // ---------- Guardar como .txt ----------
    String contenidoTxt(){
        String sep="----------------------------------------\n";
        return "CREDENCIAL DIGITAL CESBA\n"+sep
            +"Tipo: "+tvTipo.getText()+"\n"
            +"Nombre: "+tvNombre.getText()+"\n"
            +"Matrícula: "+tvMatricula.getText()+"\n"
            +"Carrera: "+tvCarrera.getText()+"\n"
            +"Semestre/Cuatrimestre: "+tvSemestre.getText()+"\n"
            +"Grupo: "+tvGrupo.getText()+"\n"
            +"Correo: "+tvCorreo.getText()+"\n"+sep
            +"Datos del código QR:\n"+payload+"\n";
    }
    void guardarTxt(){
        if(payload==null){ Toast.makeText(this,"Primero genera tu credencial",Toast.LENGTH_SHORT).show(); return; }
        try{
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TITLE,"credencial_"+tvMatricula.getText().toString().replaceAll("[^A-Za-z0-9-]","")+".txt");
            startActivityForResult(i,TXT);
        }catch(Exception e){ Toast.makeText(this,"No se pudo abrir el selector de archivos",Toast.LENGTH_SHORT).show(); }
    }
    void escribirTxt(Uri uri){
        try(java.io.OutputStream os=getContentResolver().openOutputStream(uri)){
            if(os==null) throw new java.io.IOException("sin salida");
            os.write(contenidoTxt().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Toast.makeText(this,"Archivo .txt guardado",Toast.LENGTH_SHORT).show();
        }catch(Exception e){ Toast.makeText(this,"No se pudo guardar el archivo",Toast.LENGTH_SHORT).show(); }
    }

    // ---------- Validación de datos ----------
    void validar(){
        String leido=qrBitmap!=null?leerQR(qrBitmap):null;
        if(leido==null){ new android.app.AlertDialog.Builder(this).setTitle("✗ QR no válido").setMessage("No se pudo leer el código QR de la credencial.").setPositiveButton("Aceptar",null).show(); return; }
        Map<String,String> qr=new HashMap<>();
        for(String parte:leido.split("\\|")){ int i=parte.indexOf('='); if(i>0) qr.put(parte.substring(0,i),parte.substring(i+1)); }
        String[] claves={"NOMBRE","MATRICULA","CARRERA","SEMESTRE","GRUPO","CORREO"};
        String[] etiquetas={"Nombre","Matrícula","Carrera","Semestre/Cuatrimestre","Grupo","Correo"};
        String[] enPantalla={tvNombre.getText().toString(),tvMatricula.getText().toString(),tvCarrera.getText().toString(),tvSemestre.getText().toString(),tvGrupo.getText().toString(),tvCorreo.getText().toString()};
        boolean ok=leido.startsWith("CESBA|"); StringBuilder sb=new StringBuilder();
        for(int i=0;i<claves.length;i++){
            boolean coincide=enPantalla[i].equals(qr.get(claves[i])); ok&=coincide;
            sb.append(coincide?"✓ ":"✗ ").append(etiquetas[i]).append(": ").append(enPantalla[i]).append("\n");
        }
        sb.append("\nEstado: ").append(ok?"VIGENTE":"LOS DATOS NO COINCIDEN");
        new android.app.AlertDialog.Builder(this).setTitle(ok?"✓ Datos válidos":"✗ Datos no válidos").setMessage("Los datos de la credencial se compararon contra su código QR.\n\n"+sb).setPositiveButton("Aceptar",null).show();
    }
}
