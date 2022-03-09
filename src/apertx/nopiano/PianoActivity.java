package apertx.nopiano;
import android.app.*;
import android.opengl.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import java.nio.*;
import javax.microedition.khronos.opengles.*;
import java.util.*;
import android.media.*;

public class PianoActivity extends Activity implements GLSurfaceView.Renderer,OnTouchListener{
 protected void onCreate(Bundle b){
  super.onCreate(b);
  glsv=new GLSurfaceView(this);
  glsv.setEGLConfigChooser(false);
  glsv.setRenderer(this);
  glsv.setOnTouchListener(this);
  setContentView(glsv);
  vert=ByteBuffer.allocateDirect(12).put(new byte[]{1,1, -1,1, 1,-1,  -1,-1, 1,-1, -1,1});
  vert.position(0);

  rand=new Random();
  piano_count=5;
  piano_tap=new boolean[piano_count];
  piano_y=new float[piano_count];
  piano_row=new int[piano_count];
  piano_yscale=1.0f/(piano_count-1);
  for(int i=0;i<piano_count;i++){
   piano_y[i]=2.0f*i*piano_yscale;
   piano_row[i]=rand.nextInt(4);
  }
  mp=MediaPlayer.create(this,R.raw.rain);
  mp.setLooping(true);
  bps=155.0f/60.0f/2.0f;
 }

 public void onSurfaceCreated(GL10 gl,javax.microedition.khronos.egl.EGLConfig conf){
  gl.glVertexPointer(2,gl.GL_BYTE,0,vert);
  gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
  gl.glClearColorx(0x1FFF,0x1FFF,0x1FFF,0x1FFF);
  gl.glHint(gl.GL_PERSPECTIVE_CORRECTION_HINT,gl.GL_FASTEST);
  gl.glHint(gl.GL_LINE_SMOOTH_HINT,gl.GL_FASTEST);
  gl.glLineWidthx(0x00040000);
  gl.glShadeModel(gl.GL_FLAT);
  gl.glDepthMask(false);
  gl.glEnable(gl.GL_CULL_FACE);
  gl.glDisable(gl.GL_DITHER);
 }
 public void onSurfaceChanged(GL10 gl,int w,int h){
  gl.glViewport(0,0,w,h);
  win_width=w;
  win_height=h;
  win_ratio=(float)w/h;
  if(win_ratio>1.0){
   tap_xscale=2.0f*win_ratio;
   tap_yscale=2.0f;
  }else{
   tap_xscale=2.0f;
   tap_yscale=2.0f*h/w;
  }
 }
 public void onDrawFrame(GL10 gl){
  time_last=time_current==0?SystemClock.uptimeMillis():time_current;
  time_current=SystemClock.uptimeMillis();
  time_dt=(time_current-time_last)/1000.f;
  if(!pause)for(int i=0;i<piano_count;i++){
    if(!piano_tap[i]&&piano_y[i]<=-1.0f){
     piano_tap[i]=true;
     piano_tile=(piano_tile+1)%piano_count;
    }
    if((piano_y[i]-=bps*time_dt)<=-1.0f-piano_yscale){
     piano_row[i]=rand.nextInt(4);
     piano_y[i]+=2.0f+2.0f*piano_yscale;
     piano_tap[i]=false;
    }
   }

  gl.glClear(gl.GL_COLOR_BUFFER_BIT);
  for(int i=0;i<piano_count;i++){
   gl.glColor4x(0x3FFF,0x3FFF,0x3FFF,0xFFFF);
   gl.glLoadIdentity();
   gl.glTranslatef(0.0f,piano_y[i]-piano_yscale*(piano_count-1),0.0f);
   gl.glDrawArrays(gl.GL_LINES,0,2);
   if(piano_tap[i])gl.glColor4x(0x2FFF,0x2FFF,0x2FFF,0xFFFF);
   else gl.glColor4x(0xBFFF,0xBFFF,0xBFFF,0xFFFF);
   gl.glLoadIdentity();
   gl.glTranslatef(piano_row[i]*0.5f-0.75f,piano_y[i],0.0f);
   gl.glScalef(0.25f,piano_yscale,1.0f);
   gl.glDrawArrays(gl.GL_TRIANGLES,0,6);
  }
  gl.glColor4x(0x7FFF,0x1FFF,0x1FFF,0xFFFF);
  gl.glLoadIdentity();
  gl.glTranslatef(0.875f,0.875f,0.0f);
  gl.glScalef(0.125f,0.125f,1.0f);
  gl.glDrawArrays(gl.GL_LINES,0,6);
 }

 public boolean onTouch(View v,MotionEvent e){
  if(e.getAction()==e.ACTION_DOWN){
   int zone=(int)(4.0f*e.getX()/win_width);
   float tapY=1.0f-2.0f*e.getY()/win_height;
   if(zone==3&&tapY>=0.75f){
    if(pause=!pause)mp.pause();
    else mp.start();
   }else if(!pause&&zone==piano_row[piano_tile]){
    if(tapY>=piano_y[piano_tile]-piano_yscale&&tapY<=piano_y[piano_tile]+piano_yscale){
     piano_tap[piano_tile]=true;
     piano_tile=(piano_tile+1)%piano_count;
    }
   }
  }
  return true;
 }

 protected void onRestoreInstanceState(Bundle b){
  super.onRestoreInstanceState(b);
  mp.seekTo(b.getInt("mp",0));
  piano_tile=b.getInt("piano_tile",0);
  piano_y=b.getFloatArray("piano_y");
  piano_tap=b.getBooleanArray("piano_tap");
  piano_row=b.getIntArray("piano_row");
 }
 protected void onSaveInstanceState(Bundle b){
  b.putInt("mp",mp.getCurrentPosition());
  b.putInt("piano_tile",piano_tile);
  b.putFloatArray("piano_y",piano_y);
  b.putBooleanArray("piano_tap",piano_tap);
  b.putIntArray("piano_row",piano_row);
  super.onSaveInstanceState(b);
 }
 protected void onResume(){
  super.onResume();
  glsv.onResume();
  mp.start();
 }
 protected void onPause(){
  mp.stop();
  glsv.onPause();
  super.onPause();
 }

 private GLSurfaceView glsv;
 private ByteBuffer vert;
 private byte piano_count;
 private int piano_tile;
 private float piano_yscale;
 private float[]piano_y;
 private boolean[]piano_tap;
 private int[]piano_row;
 private float win_width;
 private float win_height;
 private float win_ratio;
 private float tap_xscale;
 private float tap_yscale;
 private Random rand;
 private long time_current;
 private long time_last;
 private float time_dt;
 private MediaPlayer mp;
 private float bps;
 private boolean pause;
}
