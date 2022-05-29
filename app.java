package ev3Client;
import lejos.robotics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap; 

import lejos.hardware.BrickFinder;
import lejos.hardware.Keys;
import lejos.hardware.Sound;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.LCD;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.Motor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.port.SensorPort;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.InputStreamReader;
import java.net.URL;
import java.io.File;


public class ev3Client {
	public static class Clothes{
		public int color;
        public int type;

        Clothes(int color, int type){
        	this.color = color;
            this.type = type;
         }
      }
      
	private static EV3TouchSensor touch = new EV3TouchSensor(SensorPort.S1);
	private static EV3TouchSensor escapeTouch = new EV3TouchSensor(SensorPort.S3);
	public static int curState; //옷 class 저장
	public static ArrayList<Clothes> clothList = new ArrayList<Clothes>();
	public static HashMap<String, Integer> weatherMap = new HashMap<String, Integer>() {{put("Thunderstorm",0); put("Drizzle",1); put("Rain",2);  put("Snow", 3); put("Atmosphere",4); put("Clear", 5); put("Clouds", 6);}};
	public static int mode = 0;
	public static EV3 ev3 = (EV3) BrickFinder.getLocal();
	public static Keys keys = ev3.getKeys();
	private static EV3ColorSensor color_sensor = new EV3ColorSensor(SensorPort.S2);
	public static ArrayList<Integer> DcolorList = new ArrayList<Integer>(Arrays.asList(Color.BLACK, Color.BLUE, Color.CYAN, Color.BROWN, Color.DARK_GRAY,Color.GRAY));
	public static ArrayList<Integer> LcolorList = new ArrayList<Integer>(Arrays.asList(Color.LIGHT_GRAY, Color.NONE, Color.MAGENTA, Color.ORANGE,Color.PINK,Color.RED, Color.WHITE,Color.YELLOW));
	static TextLCD lcd = ev3.getTextLCD();
	public static String serverAddress = "10.0.1.12";
	public static int serverPort = 8040;
	public static Socket socket = null;
	public static DataOutputStream streamOut = null;
	public static DataInputStream streamIn = null;
      
     
	public static void fold(){   
          RegulatedMotor leftMotor = Motor.A;
          RegulatedMotor rightMotor = Motor.B;
          RegulatedMotor centerMotor = Motor.C;
          int armSpeed = 1080;
          int armDownSpeed = 100;
          int tailSpeed = 1080;
          int tailDownSpeed = 100;
          int armLUpDelay = 350;
          int armLDownDelay = 1700;
          int armRUpDelay = 330;
          int armRDownDelay = 1700;
          int tailUpDelay = 1300;
          int tailDownDelay = 2000;
 
          
         do{
            int temp = selectMode();
            if (temp == 1) break;
            
            File file1=new File("folding.wav");
            Sound.playSample(file1, Sound.VOL_MAX);   
            
            leftMotor.setSpeed(armSpeed);
             rightMotor.setSpeed(armSpeed);
             centerMotor.setSpeed(tailSpeed);
             
             leftMotor.forward();
             Delay.msDelay(armLUpDelay);
             leftMotor.stop();
             
            leftMotor.setSpeed(armDownSpeed);
             
             leftMotor.backward();
             Delay.msDelay(armLDownDelay);
             leftMotor.stop();
             
            leftMotor.setSpeed(armSpeed);
             
             rightMotor.forward();
             Delay.msDelay(armRUpDelay);
             rightMotor.stop();
             
             rightMotor.setSpeed(armDownSpeed);
             
             rightMotor.backward();
             Delay.msDelay(armRDownDelay);
             rightMotor.stop();
             
             clothList.add(new Clothes(getColor(), curState));
             
             centerMotor.forward();
             Delay.msDelay(tailUpDelay);
             centerMotor.stop();
             
             centerMotor.setSpeed(tailDownSpeed);
             
             centerMotor.backward();
             Delay.msDelay(tailDownDelay);
             centerMotor.stop();
             
            leftMotor.setSpeed(armSpeed);
             rightMotor.setSpeed(armSpeed);
             centerMotor.setSpeed(tailSpeed);
             
         }while(true);
      }
       public static void main(String arg[]) throws Exception{
           File file2=new File("intro.wav");
           Sound.playSample(file2, Sound.VOL_MAX); 
            //모드 저장
           while (true){
              
               //1번 모드선택 //2번째가 main function return 하는거//3번째가 fold function return하는거
              if (selectMode() == 1) {
                  if (socket != null) socket.close();
                  if(streamOut != null) streamOut.close();
                  if (streamIn != null) streamIn.close();
                 break;
              }
              mode = selectMode();
               if (mode == 1){
                  recommend();
                   continue; 
                   //recommend
               }
               else {
                      
                    curState = detected(); //카메라가 상의 하의 or detect 못함. 상의는 1 하의는 2 없으면 0

                    if (curState == 0){
                           continue;
                    }
                    else if (curState == 1){
                           fold();
                           
                    }
                    else{
                           //원래 있던 fold 함수 바지로 바꾼거
                      fold();
                    }
                   
               }
              // mode = 0; //mode 다시 초기화 unreachable error 땜에 잠시 주석처리
           }
          
       }


       public static int getColor(){
          int temp = color_sensor.getColorID();
          System.out.printf(" \n color id is = %d \n", temp);
          return color_sensor.getColorID();
       }

       public static int selectMode(){ //1 이면 recommend, 2 이면 fold, 0이면 아직 안고른거
          final SampleProvider sp = touch.getTouchMode();
         final SampleProvider esp = escapeTouch.getTouchMode();
         float[] touchValue = new float[touch.sampleSize()];
         float[] etouchValue = new float[escapeTouch.sampleSize()];
         sp.fetchSample(touchValue, 0)   ;
         esp.fetchSample(etouchValue, 0);
         touchValue = new float[touch.sampleSize()];
         etouchValue = new float[escapeTouch.sampleSize()];
         sp.fetchSample(touchValue, 0);
         esp.fetchSample(etouchValue, 0);
         while(touchValue[0] == 0.0 && etouchValue[0] == 0.0) {
             sp.fetchSample(touchValue, 0);
             
             esp.fetchSample(etouchValue, 0);
             Delay.msDelay(100);
          }
         if(etouchValue[0] != 0.0){
            File file3=new File("recommend_mode.wav");
            Sound.playSample(file3, Sound.VOL_MAX); 
            return 1;
         }
         else {
            File file4=new File("folding_mode.wav");
            Sound.playSample(file4, Sound.VOL_MAX); 
            return 0;
         }
       }

       public static int detected(){ //옷이 detect 됨
           //상의면 1 하의면 2 detected 안되면 0
           //이게 ML 사용하는거
         
           return 1;
       }
       
       public static void recommend() throws Exception{
           int temp = weatherMap.get(getWeather());
           int uprecommend = 0;
         int downrecommend = 0;
           if (temp == 0){
           System.out.println("Recommendation: ");
            for (int i = 0; i<clothList.size(); i++){
               if (clothList.get(i).type == 1){
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.BROWN ){
                     System.out.print("brown pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     downrecommend = 1;
                  }
               }
               if (uprecommend == 1 && downrecommend ==1){
                  return;
               }
            }
         }
         if (temp == 1){ //Drizzle
            for (int i = 0; i<clothList.size(); i++){
               if (clothList.get(i).type == 1){
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     downrecommend = 1;
                  }
               }
               if (uprecommend == 1 && downrecommend ==1){
                  return;
               }
            }
         }

         if (temp == 2){ //Rain
            for (int i = 0; i<clothList.size(); i++){
               if (clothList.get(i).type == 1){
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     downrecommend = 1;
                  }
               }
               if (uprecommend == 1 && downrecommend ==1){
                  return;
               }
            }
         }
         if (temp == 3){ //Snow
            for (int i = 0; i<clothList.size(); i++){
               if (clothList.get(i).type == 1){
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.BROWN ){
                     System.out.print("brown pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     downrecommend = 1;
                  }
               }
               if (uprecommend == 1 && downrecommend ==1){
                  return;
               }
            }
         }

         if (temp == 4){ //Atmosphere
            for (int i = 0; i<clothList.size(); i++){
               if (clothList.get(i).type == 1){
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GREEN){
                     System.out.print("blue shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("brown pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     downrecommend = 1;
                  }
               }
               if (uprecommend == 1 && downrecommend ==1){
                  return;
               }
            }
         }

         if (temp == 5){ //clear
            
            for (int i = 0; i<clothList.size(); i++){
               if (clothList.get(i).type == 1){
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.YELLOW){
                     System.out.print("yellow shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BROWN ){
                     System.out.print("brown shirt");
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.BROWN ){
                     System.out.print("brown pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     downrecommend = 1;
                  }
               }
               if (uprecommend == 1 && downrecommend ==1){
                  return;
               }
            }
         }
         if (temp == 6){ //clouds
            for (int i = 0; i<clothList.size(); i++){
               if (clothList.get(i).type == 1){
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.BROWN ){
                     System.out.print("brown pants");
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     downrecommend = 1;
                  }
               }
               if (uprecommend == 1 && downrecommend ==1){
                  return;
               }
            }
         }
//         System.out.println("no suitable recommendation");
//         System.out.println("recommending random clothes...");
//         System.out.println("recommending white shirt");
//         System.out.println("recommending black pants");
         Delay.msDelay(1000);
       }
       public static String getWeather()throws Exception {
          
           try {
              lcd.clear();
              
              socket = new Socket(serverAddress, serverPort);
              lcd.clear();
              
              streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
              streamOut = new DataOutputStream(socket.getOutputStream());
              
           }catch(UnknownHostException uhe) {
              lcd.drawString("Host unknown: "+uhe.getMessage(), 1, 1);
           }
           String sendM = "";
           String recvM = "";
           int cnt =0;
              try{
                 cnt += 1;
                 sendM = "fuck this up" + cnt;
                 streamOut.writeUTF(sendM);
                 streamOut.flush();
                 
                 recvM = streamIn.readUTF();
                 System.out.printf("weather is %s", recvM);
                 
              Thread.sleep(1000);
              }catch(IOException ioe){
                 lcd.drawString("Sending error: "+ioe.getMessage(), 1, 4);
              }
           return recvM;
       }      
}