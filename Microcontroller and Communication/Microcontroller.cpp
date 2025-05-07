#include <Arduino.h>
#include <DallasTemperature.h>
#include <OneWire.h>
#include <Wifi.h>
#include <time.h>

#include <string>
#include <cstdlib>

#define SCHEDULE_SIZE 5 //system set for 5 max time schedules in the system. could be modified to have more

using namespace std;

const char *wifi_ID = "NTGR_26A4_5G"; //name of NETGEAR router Wi-Fi
const char *password = "wp2aVA7s"; //password for NETGEAR router Wi-Fi
const char *ntpServer = "pool.ntp.org"; //time server to connect to in order to get local time.
const char *compServerIP = "192.168.1.180"; //IP address for socket communication with server program

static bool connectFlag = false; //indicate if the ESP32 client socket is connected to server socket
static char serverCommand[256] = ""; //used to store messages from server

WiFiClient client; //creates a client socket

TaskHandle_t connection_task; //allows for more effecient code by doing socket checking and socket handling by core 0, while the rest of the code is handled by core 1. ESP32 is a dual core system

static struct tm schedule_times[SCHEDULE_SIZE]; //create a tm struct array to contain all time schedules in
static int number_of_schedules = SCHEDULE_SIZE; //keep track of current number of schedules on microcontroller
static int onTimeMin = 15; //time that the recording device is on for, which is initialized as 15 min

static double utc_offset = -6*3600; //time displayed as military time. adjusted from greenwich mean time(utc) to central time(Texas time). Maybe include a function where this is adjustable?
static double daylight_savings = 3600; //accounts for daylight savings. Figure out how to change this when daylight savings is over. Include a feature where its adjustable?

static struct tm time_ESP32; //time struct to keep track of utc time on ESP32

static float fanOnTemp = 90.0; //temperature at which the fan turns on at
static float temp; //temperature read by sensor

//assigning GPIO pins to variables  
const int tempSensor_input = 39; //A3 pin. Used for DS18B20 temp sensor data line input
const int fanPWM = 4; //A5 pin. Used to control fan speed with PWM signal
const int fanSpeed_input = 34; //A2 pin. Used to monitor fan speed
const int fan_power = 27; //27 pin. Used for fan power signal to relay switch
const int SQM_power = 15; //15 pin. Used for SQM power signal to relay switch
const int miniPC_power = 32; //32 pin. Used for mini-PC power signal to relay switch
const int dewHeater_power = 14; //14 pin. Used for dew heater power signal to relay switch 


// put function declarations here:
void temp_read(); //record internal temperature of enclosure using DS18B20 temp sensor.
void time_read(); //take time from NTP server.
void fan_read(); //reads the speed of the fan
void power_boolean_read(); //reads if the power is supplied to recording device and/or fan
void WiFi_initializing(); //connects ESP32 to the NETGEAR WiFi
void socket_connection(void*); //socket connection from ESP32 to a remote computer. checks to see if the socket communication is still active. Need to figure out how to connect between two networks.
void communication(); //recieves a message from server and passes it to control_menu function
void control_menu(char*); //menu to select microcontroller functions from if socket communication was established
void power_menu(); //sub-menu for power functions
void fanSpeedChange(); //changing the speed fo the fan when it is powered on
void timeMenu(); //sub-menu for time schedule functions
void tempChange(); //changing the temperature condition
void time_check(); //turn on recording device at the scheduled time
void temp_check(); //turn on fan if it is too hot
void record_on(); //turn on recording device if signal is recieved from server
void fan_on(); //turn on fan if signal is recieved from server
void record_off(); //trun off fan if signal is recieved from server
void fan_off(); //turn off fan if signal is recieved from server
void time_add(); //add a new time to time schedule
void time_remove(); //remove a time from the time schedule
void time_change(); //change a current time in the time schedule
void remove_array_entry(int); //removes the time from the time_schedule array
void tm_initialization(); //creates a default set of 5 time schedules
void duration_change(); //change the duration that the recording device on for when the time schedule is triggered 
void tm_print(int); //print out the times currently being stored in the time schedule and send those times to the server 

//flags to keep fan and recording device on/off regardless of conditional statement
static bool fanFlag = false;
static bool recordFlag = false;

static bool fanOn = false;
static bool recordOn = false;


//fan speed modes
static int offSpd = 0;
static int lowSpd = 63;
static int mediumSpd = 127; 
static int highSpd = 191;
static int maxSpd = 255;

OneWire oneWire(tempSensor_input); //GPIO 39/A3 is input for digital temp reader. Argument tells OneWire(DS18B20) which pin the temp sensor data line is going to 
DallasTemperature tempSensor(&oneWire); //passes GPIO address to DallasTemperature. Pointer parameter requires this. Address points to GPIO pin 34

void setup() {
  // put your setup code here, to run once:

  Serial.begin(115200); //serial communication to terminal. 115200 is the baud rate

  //pin configuration
  pinMode(LED_BUILTIN, OUTPUT); //LED on the board. We can use this to indicate that the board is on
  pinMode(fan_power, OUTPUT); //Connect to fan relay (GPIO 27/A10 input on ADC2)
  pinMode(SQM_power, OUTPUT); //Connect to SQM relay (GPIO 15/A8 input on ADC2)
  pinMode(miniPC_power, OUTPUT); //Connect to MiniPC relay (GPIO 32/A7 on ADC1/32KHz crystal)
  pinMode(dewHeater_power, OUTPUT); //Connect to dew heater (GPIO 14/A6 on ADC2)
  pinMode(fanPWM, OUTPUT); //Connects fan PWM (fan speed output) to GPIO 4/A5
  pinMode(fanSpeed_input, INPUT); //Connects fan speed data read to A2/GPIO 34

  //pin initialization
  digitalWrite(LED_BUILTIN, 0); //light on ESP32 starts as off
  digitalWrite(fan_power, 0); //fan switch starts as off
  digitalWrite(SQM_power, 0); //SQM switch starts as off
  digitalWrite(miniPC_power, 0); //mini-PC starts as off
  digitalWrite(dewHeater_power, 0); //dew heater starts as off
  analogWrite(fanPWM, lowSpd); //sets fan at low speed

  tempSensor.begin(); //intializes the DS18B20 sensor
  
  WiFi_initializing(); //Connects ESP32 to WiFi

  configTime(utc_offset, daylight_savings, ntpServer); //Configuring ESP32 time module to npt_server

  tm_initialization(); //create a set of 5 time schedules
  

  xTaskCreatePinnedToCore(
    socket_connection,      //code for task
    "SocketConnectionTask", //name of task
    10000,                 //stack size of task
    NULL,                   //put input paramaters here
    1,                      //task priority
    &connection_task,       //the struct which the xTaskCreatePinnedToCore information is passed to
    0                       //Which core the task will run in
  );//Sets up the task assignment to the core

}

void loop() { //NOTE: everything else besides the task is being ran on Core 1 I think
  // put your main code here, to run repeatedly:

  digitalWrite(LED_BUILTIN, HIGH);
  
  time_check(); //read the time
  temp_check(); //read internal temperature


  //digitalWrite(27, LOW);
  //digitalWrite(15, LOW);
  //digitalWrite(32, LOW);
  //digitalWrite(14, LOW);
  delay(500);
  digitalWrite(LED_BUILTIN, LOW);

  //digitalWrite(27, HIGH);
  //digitalWrite(15, HIGH);
  //digitalWrite(32, HIGH);
  //digitalWrite(14, HIGH);
  delay(500); //delays translate to a 1 second clock update (500ms + 500ms = 1s )
}

// put function definitions here:
void tm_initialization(void){ //finished
  schedule_times[0].tm_hour = 18; //set in military time (so 18 means 6pm)
  schedule_times[0].tm_min = 0;
  schedule_times[0].tm_sec = 0;

  schedule_times[1].tm_hour = 23;
  schedule_times[1].tm_min = 0;
  schedule_times[1].tm_sec = 0;

  schedule_times[2].tm_hour = 0;
  schedule_times[2].tm_min = 0;
  schedule_times[2].tm_sec = 0;

  schedule_times[3].tm_hour = 1;
  schedule_times[3].tm_min = 0;
  schedule_times[3].tm_sec = 0;

  schedule_times[4].tm_hour = 2;
  schedule_times[4].tm_min = 0;
  schedule_times[4].tm_sec = 0;
}

void temp_check(void) { //finished
  tempSensor.requestTemperatures(); //gets data from the temperature sensor
  temp = tempSensor.getTempFByIndex(0); //converts temperature sensor data into Farenheit

  Serial.print("Temperature: ");
  Serial.println(temp);

  if((temp>=fanOnTemp) || (fanFlag)){ //This is to turn on fan if its too hot in the enclosure as determined by the DS18B20 sensor, OR if the server program has set the fan to stay on
    digitalWrite(fan_power, HIGH);
    fanOn = true;
  }
  else{
    digitalWrite(fan_power, LOW);
    fanOn = false;
  }
}

void time_check(void){ //finished
  int i;

  Serial.print("Time: ");
  getLocalTime(&time_ESP32); //gets time from ntp server
  Serial.print(time_ESP32.tm_hour); //print time in hour:minute:second format
  Serial.print(":");
  Serial.print(time_ESP32.tm_min);
  Serial.print(":");
  Serial.println(time_ESP32.tm_sec);
  
  for(i=0; i<SCHEDULE_SIZE; i++){ //this method for turning on at a schedule can be improved
    //if it is the scheduled hour, the recording device will turn on for certain amount of time according to onTimeMin. OR the recording device will turn on if the server program enables it
    if(((time_ESP32.tm_hour == schedule_times[i].tm_hour) && (time_ESP32.tm_min < onTimeMin)) || (recordFlag)){ //change this to account for minutes spilling over
      digitalWrite(SQM_power, HIGH);
      digitalWrite(dewHeater_power, HIGH);
      digitalWrite(miniPC_power, HIGH);
      recordOn = true;
      break;
    }
    else{
      digitalWrite(SQM_power, LOW);
      digitalWrite(dewHeater_power, LOW);
      digitalWrite(miniPC_power, LOW);
      recordOn = false;
    }
  }
}

void WiFi_initializing(void){ //finished
  int fail_count = 0;

  WiFi.begin(wifi_ID, password); //tries to connect to a Wi-Fi network
  Serial.print("Connecting to ");
  Serial.print(wifi_ID);
  Serial.print("...");
  while (WiFi.status()!=WL_CONNECTED){ //waits to get connected to a network. If it takes too long, the program will exit and continuously reset and try again until a connection is established
    if(fail_count >= 20){
      Serial.println("");
      Serial.print("WiFi connection is taking too long! Please look at WiFi connection! Retrying program...");
      delay(10000);
      abort(); //if it's taking too long to connect to WiFi, end program
    }
    else{
      fail_count++;
    }

    Serial.print(".");
    digitalWrite(LED_BUILTIN, HIGH);
    delay(250);
    digitalWrite(LED_BUILTIN, LOW);
    delay(250);
  }

  Serial.println();
  Serial.println("Connected!");
  digitalWrite(LED_BUILTIN, HIGH);
  delay(3000);
  digitalWrite(LED_BUILTIN, LOW);
  delay(3000);
}

void socket_connection(void *taskParamaters){ //finished
  //bool functionCall = false;
  int i = 0;
  while(true){ //infinite loop to run task in
    delay(1000); //1 second delay to prvent watchdog trigger
    while(!connectFlag){ //while ESP32 is disconnected from control center
      connectFlag = client.connect(compServerIP, 8080); //connects to server program via socket connection
      if(!connectFlag){
        Serial.println("No server connection!");
      }

      else{
        Serial.println("ESP32 client has connected to a server!");
      }
    }

    connectFlag = client.connected(); //continuously checks to see if socket communication is still active
    if(!connectFlag){
      Serial.println("Computer has disconnected from ESP32!");
      client.stop(); //ends socket connection
    }

    else{
      Serial.println("Communicating with Server...");
      while(client.connected()){
        delay(1000);
        communication();
      }
    }

  }
}

void communication(void){ //finished
  Serial.println("COMMUNICATION");
  if (client.available()){
    serverCommand[0] = client.read(); //recieves message from server program
    //Serial.println(serverCommand);
    control_menu(serverCommand); //passes message from server program to control_menu
  }
    
   
    memset(serverCommand, 0, sizeof(serverCommand)); //clears the serverCommand character array for the next use

  }

void control_menu(char* command){
  bool exitMenu = false;
  char exitCommand = ' ';
  Serial.println("control_menu called");
  Serial.println(command);
  switch(command[0]){
    case '0': //case 0 finished
      while(true){
        delay(1000);
        if(client.available()){
          exitCommand = client.read();
          client.flush();
        }
        if((exitCommand == '0') || (!client.connected())){
          break;
        }
        power_boolean_read();
        delay(1000);
        temp_read();
        delay(1000);
        time_read();
        fan_read();
        delay(1000);
      }
      //sends system info to server
      break;
    case '1': //case 1 finished
      fanSpeedChange();
      //change fan speed
      break;
    case '2': //case 2 finished
      client.write(to_string(fanOnTemp).c_str());
      client.flush();
      while(true){
        delay(1000);
        if(client.available()){
          exitCommand = client.read();
          client.flush();
        }
        if((exitCommand =='0') || (!client.connected())){
          break;
        }
        if(exitCommand == '1'){
          tempChange();
        }
        exitCommand = ' ';
      }
      //change temp condition
      break;
    case '3':
      timeMenu();
      //change time schedule
      break;
    case '4': //case 4 finished
      power_menu();
      //go to power system menu
      break;
    default: //default case done
      Serial.println("Invalid Command. Please Try Again");
      break;
 
  }
  exitCommand = ' ';
  client.flush();
}

void temp_read(void){ //finished
  Serial.println("temp_read called");
  //Serial.println(to_string(temp).c_str());
  client.write(to_string(temp).c_str()); //sends system temperature to server. needs to be sent as a character array otherwise issues occur
}

void time_read(void){ //finished
  Serial.println("time_read called");
  //Serial.println(asctime(&time_ESP32));
  client.write(asctime(&time_ESP32)); //sends current time of system to the server
  
}

void fan_read(void){
  Serial.println("fan_read called");
  //Serial.println(to_string(analogRead(fanSpeed_input)).c_str());
  client.write(to_string(analogRead(fanSpeed_input)).c_str()); //sends fan speed data to server. needs to be sent as a character array otherwise issues occur
} 

void power_boolean_read(void){ //finished
  Serial.println("power_boolean_read called");
  client.write(to_string(recordOn).c_str()); //sends if the recording device is on. needs to be sent as a character array otherwise issues occur
  delay(1000);
  client.write(to_string(fanOn).c_str()); //sends if fan is on. needs to be sent as a character array otherwise issues occur
}

void tempChange(void){ //finished
  Serial.println("tempChange called");
  char tempChange[256] = "";
  int i=0;
  char changeCommand;
  bool exitMenu = false;

  client.flush();
  while(true){
    delay(1000);
    if(client.available()){
      while(client.available()){
          Serial.println(i);
          tempChange[i]=client.read(); //recieves message from server, which gives the new temperature condition
          i++;
      }
      break;
    }
  }
  Serial.println(tempChange);
  fanOnTemp = atof(tempChange); //changes the temperature condition based on message recieved from server
  client.write(to_string(fanOnTemp).c_str());
  memset(tempChange, 0, sizeof(tempChange));

  }

void fanSpeedChange(void){ //finished
  Serial.println("fanSpeedChange called");
  int i=0;
  bool exitLoop = false;
  char speedChange[1] = "";
  while (true){
    delay(1000); //prevents watchdog trigger
    
    if(client.connected()){
      speedChange[0] = client.read(); // message recieved from server
    }
    
    if(!client.connected() || (speedChange[0] == '0')){ //exits this function
      break;
    }
   
    else if (speedChange[0]=='1'){ //fan speed sub-menu
      speedChange[0] = ' ';
      while(!exitLoop){
        delay(1000);
        speedChange[0] = client.read();
        Serial.println(speedChange);
        switch(speedChange[0]){
          case '0':
            exitLoop = true;
            Serial.println("Exiting Window");
            break;
          case '1':
            analogWrite(fanPWM, offSpd);
            break;
          case '2':
            analogWrite(fanPWM, lowSpd);
            break;
          case '3':
            analogWrite(fanPWM, mediumSpd);
            break;
          case '4':
            analogWrite(fanPWM, highSpd);
            break;
          case '5':
            analogWrite(fanPWM, maxSpd);
            break;
          default:
            break;
        }
        client.flush();
      }
    }
    else{
    }
    client.flush();
  }
  memset(speedChange, 0, sizeof(speedChange));
}

void timeMenu(void){ //not finished
  Serial.println("timeMenu called");
  client.flush();
  char timeCommand;
  bool exitLoop = false;
  while(!exitLoop){
    delay(1000); //prevents watchdog trigger
    if(!client.connected()){
      break;
    }
    timeCommand = client.read();
    switch(timeCommand){
      case '0':
        exitLoop = true;
        break;
      case '1':
      if(number_of_schedules == 0){ //if there's no time schedules, the time_remove function won't be called
      }
      else{
        time_remove();
      }
      break;
      case '2':
        if(number_of_schedules == 5){ //if there's already 5 time schedules, the time_add function won't be called
        }
        else{
          time_add();
        }
        break;
      case '3':
        time_change();
        break;
      case '4':
        duration_change();
        break;
      default:
        break;
    }
    client.flush();
  }
}

void duration_change(void){ //finished
  Serial.println("duration_change called");
  client.flush();
  client.write(to_string(onTimeMin).c_str());
  int i=0;
  while(true){
    delay(1000);
    if(client.available()){
      while(client.available()){
        serverCommand[i]=client.read();
        i++;
      }
      break;
    }
  }
  onTimeMin = atoi(serverCommand);
  Serial.print("New Duration: ");
  Serial.println(onTimeMin);
  memset(serverCommand, 0, sizeof(serverCommand));
}

void time_add(void){ //not finished
  Serial.println("time_add called");
  client.flush();
  tm_print(number_of_schedules);
  //client.write(asctime(schedule_times));
  char timeCommand[1] = ""; 
  int i = 0;
  while(true){ //this needs to be modified to support a stack data structure. FILO with the array
    delay(1000);
    if(client.available()){
      timeCommand[i] = client.read(); //recieves message from server
      break;
    }
  }
  
  while(true){
    delay(1000);
    if(client.available()){
      while(client.available()){
        serverCommand[i] = client.read(); //recieve a time from the server in an hour:minute:second format
        i++;
      }
      break;
    }
  }
  
  char *token_string = strtok(serverCommand, ":"); //splits up the server message based on the colons of the time string
  schedule_times[atoi(timeCommand)].tm_hour = atoi(token_string); //first section of the time string is the hour
  
  token_string = strtok(NULL, ":"); 
  schedule_times[atoi(timeCommand)].tm_min = atoi(token_string); //second section of the time string is the minute
  
  token_string = strtok(NULL, ":");
  schedule_times[atoi(timeCommand)].tm_sec = atoi(token_string); //last section of the time string is the second
  
  memset(serverCommand, 0, sizeof(serverCommand));
  number_of_schedules++;
}

void time_remove(void){ //not finished
  Serial.println("time_remove called");
  char timeCommand[1];
  timeCommand[0] = client.read(); //recieve message from server. NEEDS TO BE PUT IN A PROPER WHILE LOOP AND IF STATEMENT

  int x=0; //placeholder for testing purposes
  if(x==1){
    remove_array_entry(atoi(timeCommand)); //remove the time schedule selected by server
    client.write("Entry ");
    client.write(timeCommand);
    client.write(" was successfully removed!");
  }
  else{
    client.write("Entry does not exist! Please try again!");
  }
  
  memset(serverCommand, 0, sizeof(serverCommand));
  number_of_schedules--;
}

void time_change(void){ //not finished
  Serial.println("time_change called");
  char timeCommand[1];
  int i = 0;
  timeCommand[0] = client.read();
  while(client.available()){
    serverCommand[i] = client.read();
    i++;
  }

  char *token_string = strtok(serverCommand, ":");
  schedule_times[atoi(timeCommand)].tm_hour = atoi(token_string);
  
  token_string = strtok(NULL, ":"); 
  schedule_times[atoi(timeCommand)].tm_min = atoi(token_string);
  
  token_string = strtok(NULL, ":");
  schedule_times[atoi(timeCommand)].tm_sec = atoi(token_string);
  
  memset(serverCommand, 0, sizeof(serverCommand));
  timeMenu();
}

void tm_print(int i){ //not finished
  
}

void remove_array_entry(int element){ //not finished
  Serial.println("remove_array_entry is called");
  int arraySize = sizeof(schedule_times)/sizeof(schedule_times[0]); 
  for(int i=element; i<arraySize; i++){ //this for loop is to find the time schedule that the server wishes to remove, remove it, then readjust the array size
    schedule_times[i] = schedule_times[i+1];
  }
}

void power_menu(void){ //finished
  Serial.println("power_menu called");
  bool exitLoop = false;
  char power_command[1] = "";
  while(!exitLoop){
    delay(1000); //prevents watchdog trigger
    if(!client.connected()){
      break;
    }
    power_command[0] = client.read(); //recieves a message from the server which tells the microcontroller what to turn on/off
    Serial.println(power_command);
    switch(power_command[0]){
      case '0':
        exitLoop = true; //exit sub-menu
        break;
      case '1':
        record_on(); //turn on recording device
        break;
      case '2':
        record_off(); //turn off recording device
        break;
      case '3':
        fan_on(); //turn on fan
        break;
      case '4':
        fan_off(); //turn off fan
        break;
      default:
        break;
    }
    client.flush();
  }
  memset(power_command, 0, sizeof(power_command));
}

void record_on(void){ //finished
  Serial.println("record_on called");
  recordFlag = true; //turns on recording device
}

void fan_on(void){ //finished
  Serial.println("fan_on called");
  fanFlag = true; //turns fan on
}

void record_off(void){ //finished
  Serial.println("record_off called");
  recordFlag = false; //turns recording device off
}

void fan_off(void){ //finished
  Serial.println("fan_off called");
  fanFlag = false; //turns fan off
}
