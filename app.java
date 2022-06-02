package ev3Client;
import lejos.robotics.Color;
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
import java.util.ArrayList;

import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

import java.util.Arrays;
import java.util.HashMap; 
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
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

public class ev3Client {
   public static class Clothes {

      public int color;
      public int type;

      Clothes(int color, int type){
         this.color = color;
         this.type = type;
      }
   }
   public static int curState; //옷 class 저장
   public static ArrayList<Clothes> clothList = new ArrayList<Clothes>();
   public static HashMap<String, Integer> weatherMap = new HashMap<String, Integer>() {{put("Thunderstorm",0); put("Drizzle",1); put("Rain",2);  put("Snow", 3); put("Atmosphere",4); put("Clear", 5); put("Clouds", 6);}};
   public static int mode = 0;
   public static EV3 ev3 = (EV3) BrickFinder.getLocal();
   public static Keys keys = ev3.getKeys();
   private static EV3ColorSensor color_sensor = new EV3ColorSensor(SensorPort.S3);
   private static EV3ColorSensor color_sensor2 = new EV3ColorSensor(SensorPort.S4);
   public static ArrayList<Integer> DcolorList = new ArrayList<Integer>(Arrays.asList(Color.BLACK, Color.BLUE, Color.CYAN, Color.BROWN, Color.DARK_GRAY,Color.GRAY));
   public static ArrayList<Integer> LcolorList = new ArrayList<Integer>(Arrays.asList(Color.LIGHT_GRAY, Color.NONE, Color.MAGENTA, Color.ORANGE,Color.PINK,Color.RED, Color.WHITE,Color.YELLOW));
   static TextLCD lcd = ev3.getTextLCD();
   public static String serverAddress = "10.0.1.12";
   public static int serverPort = 8040;
   public static Socket socket = null;
   public static DataOutputStream streamOut = null;
   public static DataInputStream streamIn = null;
   public static EV3IRSensor infraredSensor = new EV3IRSensor(SensorPort.S2);
      
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
            int temp = contfold();
            if (temp == 3) break;
             
            File file3 = new File("folding.wav");
            Sound.playSample(file3, Sound.VOL_MAX);
 
            leftMotor.setSpeed(armSpeed);
            rightMotor.setSpeed(armSpeed);
            centerMotor.setSpeed(tailSpeed);
             
            leftMotor.forward();
            Delay.msDelay(armLUpDelay);
            leftMotor.stop();
             
            leftMotor.setSpeed(armDownSpeed);
            
            int color1 = getColor();
            
            leftMotor.backward();
            Delay.msDelay(armLDownDelay);
            leftMotor.stop();
            
            leftMotor.setSpeed(armSpeed);
             
            rightMotor.forward();
            Delay.msDelay(armRUpDelay);
            rightMotor.stop();
            
            rightMotor.setSpeed(armDownSpeed);
            
            int color2 = getColor2();
            
            rightMotor.backward();
            Delay.msDelay(armRDownDelay);
            rightMotor.stop();
                        
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
            
            if (color2 == -1){
               clothList.add(new Clothes(color2, curState));
            }
            else{
               clothList.add(new Clothes(color1, curState));
            }
            
         } while(true);
      }

      public static void main(String arg[]) throws Exception{
         File file1 = new File("intro.wav");
         Sound.playSample(file1, Sound.VOL_MAX);

         //모드 저장
         while (true){ 
            // 1 => folding 2=> recommend 3=> out
            mode = selectMode();
            if (mode == 2){
               File file23 = new File("recommending_mode.wav");
               Sound.playSample(file23, Sound.VOL_MAX);
               recommend();
               continue; 
               //recommend
            }
            if (mode == 3) {
               File file20 = new File("exit.wav");
               Sound.playSample(file20, Sound.VOL_MAX);
               if (socket != null) socket.close();
               if(streamOut != null) streamOut.close();
               if (streamIn != null) streamIn.close();
               break;
            }
            else {    
               File file22 = new File("folding_mode.wav");
               Sound.playSample(file22, Sound.VOL_MAX);

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

      public static int getColor() {
         int temp = color_sensor.getColorID();
         System.out.printf(" \n color id is = %d \n", temp);
         return color_sensor.getColorID();
      }
      
      public static int getColor2() {
          int temp = color_sensor2.getColorID();
          System.out.printf(" \n color id is = %d \n", temp);
          return color_sensor2.getColorID();
       }

      public static int selectMode() {
//         final SampleProvider sp = touch.getTouchMode();
//         final SampleProvider esp = escapeTouch.getTouchMode();
//         float[] touchValue = new float[touch.sampleSize()];
//         float[] etouchValue = new float[escapeTouch.sampleSize()];
//         sp.fetchSample(touchValue, 0)   ;
//         esp.fetchSample(etouchValue, 0);
//         touchValue = new float[touch.sampleSize()];
//         etouchValue = new float[escapeTouch.sampleSize()];
//         sp.fetchSample(touchValue, 0);
//         esp.fetchSample(etouchValue, 0);
//         while(touchValue[0] == 0.0 && etouchValue[0] == 0.0) {
//             sp.fetchSample(touchValue, 0);
//             esp.fetchSample(etouchValue, 0);
//             Delay.msDelay(100);
//         }
//         if (etouchValue[0] != 0.0){
//            File file3=new File("recommend_mode.wav");
//            Sound.playSample(file3, Sound.VOL_MAX); 
//            return 1;
//         }
//         else {
//            File file4=new File("folding_mode.wav");
//            Sound.playSample(file4, Sound.VOL_MAX); 
//            return 0;
//         }
            
               while(true){
                  final int remoteCommand = infraredSensor.getRemoteCommand(0);
                  switch (remoteCommand){
                     case 0:
                        continue;
                     case 1:
                        return 1;
                     case 2:
                        return 2;
                     case 3:
                        return 3;
                     case 4:
                        return 4;
                  }
               }  
      }

      public static int execute(){ //
         while(true){
              final int remoteCommand = infraredSensor.getRemoteCommand(0);
              switch (remoteCommand){
                 case 0:
                    continue;
                 case 1:
                    return 1;
                 case 2:
                    return 2;
                 case 3:
                    return 3;
                 case 4:
                    return 4;
              }
           }
      }
      public static int contfold(){ //
         while(true){
               File file2 = new File("folding_ask.wav");
               Sound.playSample(file2, Sound.VOL_MAX);
              final int remoteCommand = infraredSensor.getRemoteCommand(0);
              switch (remoteCommand){
                 case 0:
                    continue;
                 case 1:
                    return 1;
                 case 2:
                    return 2;
                 case 3:
                    return 3;
                 case 4:
                    return 4;
              }
           }
         
      }

      public static int detected(){ //옷이 detect 됨
         public static String getWeather()throws Exception {
           try {
              
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
                  sendM = "0";
                  streamOut.writeUTF(sendM);
                  streamOut.flush();
                 
                  recvM = streamIn.readUTF();
                  File file6 = new File("current_weather.wav");
                  Sound.playSample(file6, Sound.VOL_MAX);

                  if (recvM.equalsIgnoreCase("clear")) {
                     File file7 = new File("clear.wav");
                     Sound.playSample(file7, Sound.VOL_MAX);
                  }
                  else if (recvM.equalsIgnoreCase("drizzle")) {
                     File file8 = new File("drizzle.wav");
                     Sound.playSample(file8, Sound.VOL_MAX);
                  }
                  else if (recvM.equalsIgnoreCase("rain")) {
                     File file9 = new File("rain.wav");
                     Sound.playSample(file9, Sound.VOL_MAX);
                  }
                  else if (recvM.equalsIgnoreCase("snow")) {
                     File file10 = new File("snow.wav");
                     Sound.playSample(file10, Sound.VOL_MAX);
                  }
                  else if (recvM.equalsIgnoreCase("thunderstorm")) {
                     File file24 = new File("thunderstorm.wav");
                     Sound.playSample(file24, Sound.VOL_MAX);
                  }
                  else if (recvM.equalsIgnoreCase("atmosphere")) {
                     File file11 = new File("atmoosphere.wav");
                     Sound.playSample(file11, Sound.VOL_MAX);
                  }
                  else if (recvM.equalsIgnoreCase("clounds")) {
                     File file12 = new File("clouds.wav");
                     Sound.playSample(file12, Sound.VOL_MAX);
                  }
                  else {
                     System.out.printf("weather is %s", recvM);
                  }

                  Thread.sleep(1000);
               } catch(IOException ioe){
                  lcd.drawString("Sending error: "+ioe.getMessage(), 1, 4);
               }
            if (ecvM.equalsIgnoreCase("Top")){
               return 1;
            }
            else if (ecvM.equalsIgnoreCase("Bottom")){
               return 2;
            }
            else{
               System.out.println("Other detected");
               return 0; 
            }
         }      
      }
       
      public static void recommend() throws Exception{
         File file4 = new File("recommending.wav");
         Sound.playSample(file4, Sound.VOL_MAX);
         int temp = weatherMap.get(getWeather());
         int uprecommend = 0;
         int downrecommend = 0;
         if (temp == 0){
            System.out.println("Recommendation: ");
            File file5 = new File("recommend_ask.wav");
            Sound.playSample(file5, Sound.VOL_MAX);
            for (int i = 0; i<clothList.size(); i++){
               if (clothList.get(i).type == 1){
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black shirt");
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     File file17 = new File("white.wav");
                     Sound.playSample(file17, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.BROWN ){
                     System.out.print("brown pants");
                     File file18 = new File("brown.wav");
                     Sound.playSample(file18, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);                     
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
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
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);   
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     File file17 = new File("white.wav");
                     Sound.playSample(file17, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue pants");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);         
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
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);   
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     File file17 = new File("white.wav");
                     Sound.playSample(file17, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);         
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
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
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);   
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     File file17 = new File("white.wav");
                     Sound.playSample(file17, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.BROWN ){
                     System.out.print("brown pants");
                     File file18 = new File("brown.wav");
                     Sound.playSample(file18, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);         
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
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
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GREEN){
                     System.out.print("blue shirt");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);   
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     File file17 = new File("white.wav");
                     Sound.playSample(file17, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("brown pants");
                     File file18 = new File("brown.wav");
                     Sound.playSample(file18, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);         
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
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
                     File file19 = new File("blue.wav");
                     Sound.playSample(file19, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BROWN ){
                     System.out.print("brown shirt");
                     File file18 = new File("brown.wav");
                     Sound.playSample(file18, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     File file17 = new File("white.wav");
                     Sound.playSample(file17, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.BROWN ){
                     System.out.print("brown pants");
                     File file18 = new File("brown.wav");
                     Sound.playSample(file18, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);         
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
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
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.BLUE ){
                     System.out.print("blue shirt");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray shirt");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);   
                     uprecommend = 1;
                  }
                  else if (upcolor == Color.WHITE){
                     System.out.print("white shirt");
                     File file17 = new File("white.wav");
                     Sound.playSample(file17, Sound.VOL_MAX);
                     File file13 = new File("top.wav");
                     Sound.playSample(file13, Sound.VOL_MAX);
                     uprecommend = 1;
                  }
               }
               else{
                  int upcolor = clothList.get(i).color;
                  if (upcolor == Color.BLACK){
                     System.out.print("black pants");
                     File file15 = new File("black.wav");
                     Sound.playSample(file15, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.BROWN ){
                     System.out.print("brown pants");
                     File file18 = new File("brown.wav");
                     Sound.playSample(file18, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
                  else if( upcolor == Color.GRAY ){
                     System.out.print("gray pants");
                     File file25 = new File("grey.wav");
                     Sound.playSample(file25, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);         
                     downrecommend = 1;
                  }
                  else if (upcolor == Color.BLUE){
                     System.out.print("blue pants");
                     File file16 = new File("blue.wav");
                     Sound.playSample(file16, Sound.VOL_MAX);
                     File file14 = new File("bottom.wav");
                     Sound.playSample(file14, Sound.VOL_MAX);
                     downrecommend = 1;
                  }
               }
               if (uprecommend == 1 && downrecommend ==1){
                  return;
               }
            }
         }
            File file21 = new File("random.wav");
            Sound.playSample(file21, Sound.VOL_MAX);
//         System.out.println("no suitable recommendation");
//         System.out.println("recommending random clothes...");
//         System.out.println("recommending white shirt");
//         System.out.println("recommending black pants");
         Delay.msDelay(1000);
       }
       public static String getWeather()throws Exception {
           try {
              
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
                  sendM = "1";
                  streamOut.writeUTF(sendM);
                  streamOut.flush();
                 
                  recvM = streamIn.readUTF();
                  System.out.printf("weather is %s", recvM);
                 
                  Thread.sleep(1000);
               } catch(IOException ioe){
                  lcd.drawString("Sending error: "+ioe.getMessage(), 1, 4);
               }
            return recvM;
       }      
}