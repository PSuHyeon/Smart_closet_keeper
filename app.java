package sandbox;
import lejos.robotics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap; 

import lejos.hardware.BrickFinder;
import lejos.hardware.Keys;
import lejos.hardware.Sound;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.Motor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.port.SensorPort;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.utility.Delay;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.io.InputStreamReader;
import java.net.URL;

public class sandbox {
	
		private static EV3TouchSensor touch = new EV3TouchSensor(SensorPort.S1);
		private static EV3TouchSensor escapeTouch = new EV3TouchSensor(SensorPort.S3);
	    public static int curState; //옷 class 저장
	    public static ArrayList<Integer> clothList = new ArrayList<Integer>();
	    public static HashMap<String, Integer> weatherMap = new HashMap<String, Integer>() {{put("Thunderstorm",0); put("Drizzle",1); put("Rain",2);  put("Snow", 3); put("Atmosphere",4); put("Clear", 5); put("Clouds", 6);}};
	    public static int mode = 0;
	    public static EV3 ev3 = (EV3) BrickFinder.getLocal();
	    public static Keys keys = ev3.getKeys();
	    private static EV3ColorSensor color_sensor = new EV3ColorSensor(SensorPort.S2);
	    public static ArrayList<Integer> DcolorList = new ArrayList<Integer>(Arrays.asList(Color.BLACK, Color.BLUE, Color.CYAN, Color.BROWN, Color.DARK_GRAY,Color.GRAY));
	    public static ArrayList<Integer> LcolorList = new ArrayList<Integer>(Arrays.asList(Color.LIGHT_GRAY, Color.NONE, Color.MAGENTA, Color.ORANGE,Color.PINK,Color.RED, Color.WHITE,Color.YELLOW));
        
	    
		
	  
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
		    Sound.playNote(Sound.PIANO, 523, 1000);
		    
			do{
				int temp = selectMode();
				if (temp == 1) break;
				
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
			    
			    clothList.add(getColor());
			    
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

	         //모드 저장

	        while (true){
	        	
	        	 //1번 모드선택 //2번째가 main function return 하는거//3번째가 fold function return하는거
	        	if (selectMode() == 1) break;
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
	        return color_sensor.getColorID();
	    }

	    public static int selectMode(){ //1 이면 recommend, 2 이면 fold, 0이면 아직 안고른거
	    	final SampleProvider sp = touch.getTouchMode();
			final SampleProvider esp = escapeTouch.getTouchMode();
			float[] touchValue = new float[touch.sampleSize()];
			float[] etouchValue = new float[escapeTouch.sampleSize()];
			sp.fetchSample(touchValue, 0)	;
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
				Sound.playNote(Sound.FLUTE, 1000, 1000);
				return 1;
			}
			else {
				Sound.playNote(Sound.FLUTE, 300, 1000);
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
	        System.out.printf("size of colorlist = %d",clothList.size());
	        System.out.printf("size of colorlist = %d",clothList.get(0));
	        if (temp >= 5)
	        {
	            for(int i = 0; i<clothList.size();i++){
	                if (LcolorList.contains(clothList.get(i))){
	                    System.out.printf("%s",clothList.get(i));
	                }
	            }
	        }
	        if (temp < 5)
	        {
	            for(int i = 0; i<clothList.size();i++){
	                if (DcolorList.contains(clothList.get(i))){
	                    System.out.printf("%s",clothList.get(i));
	                }
	            }
	        }
	        
	       
	    }
	    public static String getWeather()throws Exception {
	        URL openweatherURL = new URL( "https://api.openweathermap.org/data/2.5/weather?lat=36&lon=127&appid=99162b3225b63083cfc32b51154370bf" );
	        HttpURLConnection conn = (HttpURLConnection) openweatherURL.openConnection();
	        conn.setRequestMethod("GET");
	        conn.setRequestProperty("Content-type", "application/json");
	        conn.setDoOutput(true);
	        try{
	            StringBuffer sb = new StringBuffer();
	            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
	            String temp = " "; 
	            while(br.ready()) {
	                sb.append(br.readLine());
	                int weatherStartIndex = sb.indexOf("main") + 7; 
	                int weatherEndIndex = weatherStartIndex + sb.substring(weatherStartIndex).indexOf('"');
	                temp = sb.substring(weatherStartIndex, weatherEndIndex);
	            }
	            conn.disconnect();
	            return temp;
	        }catch(Exception e) {
	            e.printStackTrace();
	            return " ";
	        }
	    	return "Clear";
	    }
}