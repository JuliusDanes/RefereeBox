import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.net.*; 
import krister.Ess.*; 
import org.json.*; 
import java.io.*; 
import java.io.BufferedWriter; 
import java.io.FileWriter; 
import java.io.File; 
import java.util.zip.*; 
import org.json.*; 

import krister.Ess.*; 
import org.json.*; 
import org.json.zip.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class mslrb2015 extends PApplet {

/* ==================================
   MSL RefBox 2015 (Processing 3)
       LMFerreira
       RDias
       FAmaral 
       BCunha
   ================================== */




public static final String MSG_VERSION="1.2.3";
public static final String MSG_VERSION_MSG="(RoboCup 2016)";
public static final String MSG_WINDOWTITLE="RoboCup MSL RefBox 2015 - "+MSG_VERSION+" "+MSG_VERSION_MSG;
public static final String MSG_HALFTIME="End Current Part ?";
public static final String MSG_RESET="Reset Game ?";
public static final int appFrameRate = 15;

public static String[] Teamcmds= { "KickOff", "FreeKick", "GoalKick", "Throw In", "Corner", "Penalty", "Goal", "Repair", "Red", "Yellow" };
public static String[] Commcmds= { "START", "STOP", "DropBall", "Park", "End Part",  "RESET", "EndGame" };

public static final String[] cCTeamcmds= { "K", "F", "G", "T", "C", "P", "A", "O", "R", "Y" };
public static final String[] cMTeamcmds= { "k", "f", "g", "t", "c", "p", "a", "o", "r", "y" };
public static final int CMDID_TEAM_KICKOFF = 0;
public static final int CMDID_TEAM_FREEKICK = 1;
public static final int CMDID_TEAM_GOALKICK = 2;
public static final int CMDID_TEAM_THROWIN = 3;
public static final int CMDID_TEAM_CORNER = 4;
public static final int CMDID_TEAM_PENALTY = 5;
public static final int CMDID_TEAM_GOAL = 6;
public static final int CMDID_TEAM_REPAIR_OUT = 7;
public static final int CMDID_TEAM_REDCARD = 8;
public static final int CMDID_TEAM_YELLOWCARD = 9;

public static final String[] cCommcmds= { "s", "S", "N", "L", "h", "Z", "e" };  
public static final int CMDID_COMMON_START = 0;
public static final int CMDID_COMMON_STOP = 1;
public static final int CMDID_COMMON_DROP_BALL = 2;
public static final int CMDID_COMMON_PARKING = 3;
public static final int CMDID_COMMON_HALFTIME = 4;
public static final int CMDID_COMMON_RESET = 5;
public static final int CMDID_COMMON_ENDGAME = 6;

public static ScoreClients scoreClients = null;
public static MSLRemote mslRemote = null;
public static MyServer BaseStationServer;
public static Client connectingClient = null;

public static Team teamA,teamB;
public static Button[] bTeamAcmds = new Button[CMDID_TEAM_YELLOWCARD + 1];
public static Button[] bTeamBcmds = new Button[CMDID_TEAM_YELLOWCARD + 1];
public static Button[] bCommoncmds = new Button[CMDID_COMMON_RESET + 1];
public static BSliders[] bSlider = new BSliders[4];

public static Table teamstable;
public static TableRow teamselect;
public static long updateScoreClientslasttime=0;
public static long tstartTime=0, tsplitTime=0, tprevsplitTime=0;
public static boolean TESTMODE=false, stopsplittimer=true, VOICECOACH=false, REMOTECONTROLENABLE=false;
public static char LastKickOff='.';
public static String[] Last5cmds= { ".", ".", ".", ".", "." };
public static String LogFileName;
public static String lastaction=".";
public static String gametime = "", gameruntime = "";

//GUI
public static Button[] bPopup = new Button[2];
public static PVector offsetLeft= new PVector(230, 180);
public static PVector offsetRight= new PVector(760, 180);
public static PFont buttonFont, clockFont, panelFont, scoreFont, debugFont, teamFont, watermark;
// public static PImage backgroundImage;
public PImage backgroundImage;
public PImage rcfLogo;

// Sounds
public static AudioChannel soundMaxTime;
public static long lastPlayMillis = 0;

public static PApplet mainApplet = null;

/**************************************************************************************************************************
 * This the Processing setup() function
 * The setup() function is called once when the program starts.
 * It's used to define initial enviroment properties such as screen size and background color and to load media
  such as images and fonts as the program starts.
 * There can only be one setup() function for each program and it shouldn't be called again after its initial execution.
 * Note: Variables declared within setup() are not accessible within other functions, including draw().
**************************************************************************************************************************/
public void setup() {
  mainApplet = this;
  
  backgroundImage = loadImage("img/bg_normal.png");
  
  surface.setTitle(MSG_WINDOWTITLE); 
  clockFont = createFont("fonts/LCDM.TTF", 64, false);
  scoreFont = createFont("fonts/LED.ttf", 40, false);
  buttonFont=loadFont("fonts/Futura-CondensedExtraBold-24.vlw");
  teamFont=loadFont("fonts/Futura-CondensedExtraBold-52.vlw");
  panelFont=loadFont("fonts/Futura-CondensedExtraBold-20.vlw");
  debugFont=loadFont("fonts/Monaco-14.vlw");
  watermark=createFont("Arial", 112, false);
  
  createDir(mainApplet.dataPath("tmp/"));
  createDir(mainApplet.dataPath("logs/"));
  
  //==============================================
  //=== Modules Initialization
  Config.Load(this, "config.json");                                     // Load config file
  Log.init(this);                                                       // Init Log module
  comms_initDescriptionDictionary();                                    // Initializes the dictionary for communications with the basestations 

  scoreClients = new ScoreClients(this, Config.scoreServerPort);        // Load score clients server
  BaseStationServer = new MyServer(this, Config.basestationServerPort); // Load basestations server
  mslRemote = new MSLRemote(this, Config.remoteServerPort);             // Load module for MSL remote control
  
  println("This IP: "+Server.ip());
  teamA = new Team(Config.defaultCyanTeamColor,true);                   // Initialize Cyan team (Team A)
  teamB = new Team(Config.defaultMagentaTeamColor,false);               // Initialize Magenta team (Team B)
  teamstable = loadTable("msl_teams.csv", "header");                    // Load teams table
  
  //==============================================
  //=== GUI Initialization
  initGui();
  RefreshButonStatus1();
  resetStartTime(true);
  frameRate(appFrameRate);
  
  // Sounds initialization
  Ess.start(this); // start up Ess
  if(Config.sounds_maxTime.length() > 0) {
    soundMaxTime = new AudioChannel(dataPath("sounds/" + Config.sounds_maxTime));
  }else{
    soundMaxTime = null;
  }
}

/**************************************************************************************************************************
  This the Processing draw() function 
  Called directly after setup(), the draw() function continuously executes the lines of code contained inside its block
  until the program is stopped or noLoop() is called. draw() is called automatically and should never be called explicitly.
  It should always be controlled with noLoop(), redraw() and loop(). If noLoop() is used to stop the code in draw() from executing, 
  then redraw() will cause the code inside draw() to be executed a single time, and loop() will cause the code inside draw() 
  to resume executing continuously.
  The number of times draw() executes in each second may be controlled with the frameRate() function
  It is common to call background() near the beginning of the draw() loop to clear the contents of the window, as shown in the first 
  example above. Since pixels drawn to the window are cumulative, omitting background() may result in unintended results, especially 
  when drawing anti-aliased shapes or text.
  There can only be one draw() function for each sketch, and draw() must exist if you want the code to run continuously, or to process 
  events such as mousePressed(). Sometimes, you might have an empty call to draw() in your program, as shown in the second example above.  
 **************************************************************************************************************************/
public void draw() {
  
  background(backgroundImage);
  
  long t1=getGameTime();
  long t2=getSplitTime();
  gametime=nf(PApplet.parseInt((t1/1000)/60), 2)+":"+nf(PApplet.parseInt((t1/1000)%60), 2);
  gameruntime=nf(PApplet.parseInt(t2/1000/60), 2)+":"+nf(PApplet.parseInt((t2/1000)%60), 2);

  //update basestations data   
  long t=System.currentTimeMillis();
  if ( (t-updateScoreClientslasttime) >= Config.scoreClientsUpdatePeriod_ms ) scoreClients.update_tTeams(gametime,gameruntime);
  //verifyremotecontrol();
  mslRemote.checkMessages();
  checkBasestationsMessages();

  for (int i = 0; i < bCommoncmds.length; i++)
    bCommoncmds[i].update();
  
  for (int i = 0; i < bTeamAcmds.length; i++) {
    bTeamAcmds[i].update();
    bTeamBcmds[i].update();
  }

  teamA.updateUI();
  teamB.updateUI();
  
  for (int i = 0; i < bSlider.length; i++)
    bSlider[i].update();

  StateMachineCheck(); // Check scheduled state change
  RefreshButonStatus1(); // Refresh buttons

  fill(255);
  textAlign(CENTER, CENTER);

  //score
  textFont(scoreFont);
  text("[  "+teamA.Score+"  -  "+teamB.Score+"  ]", 500, 25);
  //main clock
  textFont(clockFont);
  fill(255);
  text( gametime, 500, 85);
  //run clock  
  textFont(panelFont);
  text(StateMachine.GetCurrentGameStateString()+" ["+gameruntime+"]", 500, 140);
  //debug msgs  
  textFont(debugFont);
  textAlign(LEFT, BOTTOM);
  fill(0xff00ff00);
  for (int i=0; i<5; i++)
  {
    text( Last5cmds[i], 340, height-4-i*18);
    fill(0xff007700);
  }
  fill(255);
  //server info
  textAlign(CENTER, BOTTOM);
  String time=nf(hour(),2)+":"+nf(minute(),2)+":"+nf(second(),2);
  text("[ "+time+" ]        "+Server.ip()+" ["+scoreClients.clientCount()+"/"+BaseStationServer.clientCount+"]", width/2, 578);  
  
  //println(StateMachine.GetCurrentGameState().getValue());


  //==========================================

  if (Popup.isEnabled()) {
    Popup.draw();
  }

  //==========================================
  
  if(lastPlayMillis != 0 && millis() - lastPlayMillis > Config.setPieceMaxTime_ms)
  {
    //Log.logMessage("7 seconds rule!");
    lastPlayMillis = 0;
    soundMaxTime.cue(0);
    soundMaxTime.play();
  }
}

/**************************************************************************************************************************
 *   This the Processing exit() function 
 * Quits/stops/exits the program. Programs without a draw() function exit automatically after the last line has run, but programs 
 * with draw() run continuously until the program is manually stopped or exit() is run.
 * Rather than terminating immediately, exit() will cause the sketch to exit after draw() has completed (or after setup() 
 * completes if called during the setup() function).
 * For Java programmers, this is not the same as System.exit(). Further, System.exit() should not be used because closing 
 * out an application while draw() is running may cause a crash (particularly with P3D). 
/**************************************************************************************************************************/
public void exit() {
  println("Program is stopped !!!");
  
  // Reset teams to close log files
  if(teamA != null) teamA.reset();
  if(teamB != null) teamB.reset();
  
  LogMerger merger = new LogMerger(Log.getTimedName());
  //merger.merge();
  merger.zipAllFiles();
  
  // Stop all servers
  scoreClients.stopServer();
  BaseStationServer.stop();
  mslRemote.stopServer();
  
  super.exit();
}

public void initGui()
{
  //common commands
  for (int i=0; i < bCommoncmds.length; i++){
    bCommoncmds[i] = new Button(435+130*(i%2), 275+i*35-35*(i%2), Commcmds[i], 0xffFEFF00, -1, 255, 0xffFEFF00);
    
    // End part and reset need confirmation popup (don't send message right away)
    if(i <= CMDID_COMMON_PARKING) {
      bCommoncmds[i].cmd = "" + cCommcmds[i];
      bCommoncmds[i].msg = "" + Commcmds[i];
    }
  }
  bCommoncmds[0].setcolor(0xff12FF03, -1, -1, 0xff12FF03);  //Start  / green
  bCommoncmds[1].setcolor(0xffE03020, -1, -1, 0xffE03030);  //Stop  /red  #FC0303 

  for (int i=0; i<6; i++) {
    bTeamAcmds[i] = new Button(offsetLeft.x, offsetLeft.y+70*i, Teamcmds[i], 255, -1, 255, Config.defaultCyanTeamColor);
    bTeamAcmds[i].cmd = "" + cCTeamcmds[i];
    bTeamAcmds[i].msg = Teamcmds[i];
    
    bTeamBcmds[i] = new Button(offsetRight.x, offsetRight.y+70*i, Teamcmds[i], 255, -1, 255, Config.defaultMagentaTeamColor);
    bTeamBcmds[i].cmd = "" + cMTeamcmds[i];
    bTeamBcmds[i].msg = Teamcmds[i];
  }

  bTeamAcmds[6] = new Button(offsetLeft.x-135, offsetLeft.y, Teamcmds[6], Config.defaultCyanTeamColor, -1, 255, Config.defaultCyanTeamColor);   // Goal A
  bTeamAcmds[7] = new Button(offsetLeft.x-135, offsetLeft.y+70*4, Teamcmds[7], Config.defaultCyanTeamColor, -1, 255, Config.defaultCyanTeamColor); // Repair A
  bTeamAcmds[8] = new Button(offsetLeft.x-162, offsetLeft.y+70*5, "", 0xffFC0303, 0xff810303, 255, 0xffFC0303);  //red card A
  bTeamAcmds[9] = new Button(offsetLeft.x-105, offsetLeft.y+70*5, "", 0xffFEFF00, 0xff808100, 255, 0xffFEFF00);  //yellow card A
  
  bTeamBcmds[6] = new Button(offsetRight.x+135, offsetRight.y, Teamcmds[6], Config.defaultMagentaTeamColor, -1, 255, Config.defaultMagentaTeamColor);  //Goal B
  bTeamBcmds[7] = new Button(offsetRight.x+135, offsetRight.y+70*4, Teamcmds[7], Config.defaultMagentaTeamColor, -1, 255, Config.defaultMagentaTeamColor);//Repair B
  bTeamBcmds[8] = new Button(offsetRight.x+162, offsetRight.y+70*5, "", 0xffFC0303, 0xff810303, 255, 0xffFC0303);  //red card B
  bTeamBcmds[9] = new Button(offsetRight.x+105, offsetRight.y+70*5, "", 0xffFEFF00, 0xff808100, 255, 0xffFEFF00);  //yellow card B
  
  for (int i = 6; i < 10; i++) {
    bTeamAcmds[i].cmd = "" + cCTeamcmds[i];
    bTeamAcmds[i].msg = Teamcmds[i];
    bTeamBcmds[i].cmd = "" + cMTeamcmds[i];
    bTeamBcmds[i].msg = Teamcmds[i];
  }

  // OFF-state goal button (subtract goal)
  bTeamAcmds[6].msg_off = "Goal-";
  bTeamAcmds[6].cmd_off = "" + COMM_SUBGOAL_CYAN;
  bTeamBcmds[6].msg_off = "Goal-";
  bTeamBcmds[6].cmd_off = "" + COMM_SUBGOAL_MAGENTA;
  

  bTeamAcmds[8].setdim(32, 48); 
  bTeamAcmds[9].setdim(32, 48); 
  bTeamBcmds[8].setdim(32, 48);  //red C resize
  bTeamBcmds[9].setdim(32, 48);  //yellow C resize

  bPopup[0] = new Button(0, 0, "y", 255, Config.defaultCyanTeamColor, 0, Config.defaultCyanTeamColor);
  bPopup[1] = new Button(0, 0, "n", 255, Config.defaultMagentaTeamColor, 0, Config.defaultMagentaTeamColor);

  bSlider[0]=new BSliders("Testmode",420,480,true, TESTMODE);
  bSlider[1]=new BSliders("Log",420+132,480,true, Log.enable);
  bSlider[2]=new BSliders("Remote",420,480+32,Config.remoteControlEnable, REMOTECONTROLENABLE);
  bSlider[3]=new BSliders("Coach",420+132,480+32,false, VOICECOACH);
  
  buttonCSTOPactivate();
}

public boolean createDir(String dirPath)
{
    // Create logs directory if necessary
    File logsDir = new File(dirPath);
    if(!logsDir.exists() || !logsDir.isDirectory())
    {
      if(!logsDir.mkdir()){
        println("ERROR - Could not create logs directory.");
        return false;
      }
    }
    return true;
}
class BSliders {
  String Label;
  boolean enabled;
  Boolean on;
  float posx; 
  float posy;
  int c; 

  BSliders(String Label, float x, float y, boolean enable, boolean on ) { 
    this.Label=Label;
    this.posx=x;
    this.posy=y;
    this.on=on;
    this.enabled=enable;
    this.c=255;
  }

  public void update() {
    textAlign(LEFT, BOTTOM);
    rectMode(CENTER);
    strokeWeight(1);
    if (enabled) c=192;
    else c=92;
    stroke(c); noFill(); 
    rect(posx, posy, 48, 23, 12);
    fill(c); noStroke();
    textFont(debugFont);
    if (on) {
      rect(posx-8+17, posy, 26, 17, 12);//on
      fill(92);text("on", posx+2, posy+7);
    }
    else {
      rect(posx-8, posy, 26, 17, 12);//off
      fill(92); text("off", posx-19, posy+7);
    }
    fill(c);
    text(Label, posx+30, posy+7);
  }
 
 
 public boolean mouseover() {
    if ( mouseX>(posx-24-2) && mouseX<(posx+24+2) && mouseY>(posy-12-2) && mouseY<(posy+12+2) ) return true;
    return false;
 }
 
 public void toogle() {
   if (this.enabled) this.on=!on;  
 }
 
 public void enable() {
   this.enabled=true;
 }
 public void disable() {
   this.enabled=false;
 }
}
 
public void setbooleansfrombsliders() {
  TESTMODE=bSlider[0].on;
  Log.enable = bSlider[1].on;
  REMOTECONTROLENABLE=bSlider[2].on;
  VOICECOACH=bSlider[3].on; 
}
class Button {
  float x; 
  float y;
  String bStatus;  // normal, active, disabled
  Boolean HOVER;
  String Label;
  int bwidth=116; 
  int bheight=48;
  int hbwidth=bwidth/2; 
  int hbheight=bheight/2;
  int cstroke, cfill, cstrokeactive, cfillactive;
  
  public String msg = null; // long name for the command
  public String msg_off = null;
  public String cmd = null; // command (usually a char)
  public String cmd_off = null;
  
  Button(float x, float y, String Label, int c1, int c2, int c3, int c4) { 
    this.x=x;
    this.y=y;
    this.Label=Label;
    this.bStatus="disabled";
    this.HOVER=false;
    this.cstroke=c1;
    this.cfill=c2;
    this.cstrokeactive=c3;
    this.cfillactive=c4;
  }

  public void update() {
    rectMode(CENTER);
    textAlign(CENTER, CENTER);

    if (this.isEnabled() && HOVER) {  //shadow
      noFill();
      strokeWeight(4);
      stroke(0);
      rect(x+2, y+2, bwidth-2, bheight-2, 8);
    }
    strokeWeight(2);
    if (this.isEnabled()) {
      if (this.isActive()) {
        noStroke();
        if (cfillactive==-1) noFill(); 
        else fill(cfillactive);
      } else {  //not active, no hover
        if (cstroke==-1) noStroke(); 
        else stroke(cstroke);
        if (cfill==-1) noFill(); 
        else fill(cfill);
      }
    } else { //disabled
      fill(0, 8);
      stroke(96);
    }
    rect(x, y, bwidth, bheight, 8);

    textFont(buttonFont);
    if (this.isEnabled()) {
      fill(0);//shadow
      text(Label, x+2, y-2);
      if (this.isActive()) {
        if (cstrokeactive==-1) fill(255); 
        else fill(cstrokeactive);
      } else {  //not active, no hover
        if (cstroke==-1) noFill(); 
        else fill(cstroke);
      }
    } else fill(96); //disabled  
    text(Label, x, y-2);//-4
  }

  public void checkhover() {
    if ( mouseX>(x-hbwidth-2) && mouseX<(x+hbwidth+2) && mouseY>(y-hbheight-2) && mouseY<(y+hbheight+2) ) this.HOVER=true;
    else this.HOVER=false;
  }

  public boolean isDisabled() {
    if (bStatus.equals("disabled")) return true;
    else return false;
  }

  public boolean isEnabled() {
    if (bStatus.equals("disabled")) return false;
    else return true;
  }

  public boolean isActive() {
    if ( this.bStatus.equals("active") ) return true;
    else return false;
  }

  public void activate() {
    this.bStatus="active";
  }

  public void enable() {
    this.bStatus="normal";
  }

  public void disable() {
    this.bStatus="disabled";
    this.HOVER=false;
  }

  public void toggle() {
    if (this.isEnabled()) {
      if ( this.isActive() ){
        this.bStatus="normal";
        if(StateMachine.setpiece && this.Label == Teamcmds[6]) {
          StateMachine.ResetSetpiece();
          send_to_basestation(cCommcmds[1]);
        }
      }
      else this.bStatus="active";
    }
  }


  public void setcolor(int c1, int c2, int c3, int c4) {
    this.cstroke=c1;
    this.cfill=c2;
    this.cstrokeactive=c3;
    this.cfillactive=c4;
  }

  public void setdim(int w, int h) {
    bwidth=w; 
    bheight=h;
    hbwidth=bwidth/2; 
    hbheight=bheight/2;
  }
  
  public void setxy(float x, float y){    
    this.x=x;
    this.y=y;
  }

}

public static Button buttonFromEnum(ButtonsEnum btn)
{
  if(btn.getValue() <= ButtonsEnum.BTN_RESET.getValue())
    return bCommoncmds[btn.getValue()];
  
  if(btn.getValue() <= ButtonsEnum.BTN_C_YELLOW.getValue())
    return bTeamAcmds[btn.getValue() - ButtonsEnum.BTN_C_KICKOFF.getValue()];
  
  if(btn.getValue() <= ButtonsEnum.BTN_M_YELLOW.getValue())
    return bTeamBcmds[btn.getValue() - ButtonsEnum.BTN_M_KICKOFF.getValue()];
  
  return null;
}

public void buttonEvent(char group, int pos) {
  
  ButtonsEnum clickedButton = null;
  Button clickBtn = null;
   
  if (group=='C')
  {
    clickedButton = ButtonsEnum.items[pos];
    clickBtn = buttonFromEnum(clickedButton);
    if(!clickBtn.isDisabled())
      clickBtn.toggle();
    else
      clickedButton = null;
  }
  else if (group=='A')
  {
    clickedButton = ButtonsEnum.items[pos + ButtonsEnum.BTN_C_KICKOFF.getValue()];
    clickBtn = buttonFromEnum(clickedButton);
    if(!clickBtn.isDisabled())
      clickBtn.toggle();
    else
      clickedButton = null;
  }
  else if (group=='B')
  {
    clickedButton = ButtonsEnum.items[pos + ButtonsEnum.BTN_M_KICKOFF.getValue()];
    clickBtn = buttonFromEnum(clickedButton);
    if(!clickBtn.isDisabled())
      clickBtn.toggle();
    else
      clickedButton = null;
  }
  
  if(clickedButton != null)
  {
    boolean btnOn = buttonFromEnum(clickedButton).isActive();
    
    StateMachine.Update(clickedButton, btnOn);
    
    if(soundMaxTime != null && clickedButton.isStart())
      lastPlayMillis = mainApplet.millis();
    else
      lastPlayMillis = 0;
    
    if(clickedButton.isStop())
    {
      lastPlayMillis = 0;
    }
    
    // Special cases, that send only event message on game change (flags)
    if( clickedButton.isYellow() || clickedButton.isRed() || clickedButton.isRepair() )
    {
      // Do literally nothing...
    }else{
      if(clickedButton.isCommon())
      {
        event_message_v2(clickedButton, true);
      }else{
        event_message_v2(clickedButton, buttonFromEnum(clickedButton).isActive());
      }
    }
  }
}
// New accepted connections
public static void serverEvent(MyServer whichServer, Client whichClient) {
  try {
  if (whichServer.equals(BaseStationServer)) {
    Log.logMessage("New BaseStation @ "+whichClient.ip());
  }
  else if (whichServer.equals(scoreClients.scoreServer)) {
    Log.logMessage("New ScoreClient @ " + whichClient.ip());
  }
  else if (mslRemote != null && mslRemote.server != null && whichServer != null && whichServer.equals(mslRemote.server)) {
    Log.logMessage("New Remote @ " + whichClient.ip());
  }
  }catch(Exception e){}
}

// Client authentication
public static void clientValidation(MyServer whichServer, Client whichClient) {
  try{
    // BASESTATION CLIENTS AUTH
    if (whichServer.equals(BaseStationServer)) {
      if (!Popup.isEnabled()) {
        if(setteamfromip(whichClient.ip()))
          connectingClient = whichClient; // Accept client!
        else
        {
          // Invalid team
          Log.logMessage("Invalid team " + whichClient.ip());
          whichClient.write(COMM_RESET);
          whichClient.stop();
        }
      } else {
        Log.logMessage("ERR Another team connecting");
        whichClient.write(COMM_RESET);
        whichClient.stop();
      }
    }
    // SCORE CLIENTS AUTH
    else if (whichServer.equals(scoreClients.scoreServer)) {
      if(!Config.scoreServerClients.hasValue(whichClient.ip())) {
        Log.logMessage("Reject ScoreClient " + whichClient.ip());
        whichClient.stop();
      }
    }
    // REMOTE CLIENTS AUTH
    else if (mslRemote != null && mslRemote.server != null && whichServer.equals(mslRemote.server)) {
      
    }
  }catch(Exception e){}
}


public static void send_to_basestation(String c){
  println("Command "+c+" :"+Description.get(c+""));
  BaseStationServer.write(c);
  
//  if(!c.equals("" + COMM_WORLD_STATE))
//  {
    Log.logactions(c);
    mslRemote.setLastCommand(c);      // Update MSL remote module with last command sent to basestations
//  }
}

public static void event_message_v2(ButtonsEnum btn, boolean on)
{
  String cmd = buttonFromEnum(btn).cmd;
  String msg = buttonFromEnum(btn).msg;
  if(!on)
  {
    cmd = buttonFromEnum(btn).cmd_off;
    msg = buttonFromEnum(btn).msg_off;
  }
  
  Team t = null;
  if(btn.isCyan()) t = teamA;
  if(btn.isMagenta()) t = teamB;
  
  if(cmd != null && msg != null)
  {
    send_event_v2(cmd, msg, t);
  }
}

public static void send_event_v2(String cmd, String msg, Team t)
{
  //println("EVENT, " + cmd + " / " + msg);
  String teamName = (t != null) ? t.longName : "";
  send_to_basestation(cmd);
  scoreClients.update_tEvent(cmd, msg, teamName);
  mslRemote.update_tEvent(cmd, msg, t);
}

public void event_message(char team, boolean on, int pos) {
  if (on) {  //send to basestations
    if (team=='C' && pos<4){
      send_to_basestation(cCommcmds[pos]);
      scoreClients.update_tEvent("" + cCommcmds[pos], Commcmds[pos], "");
      mslRemote.update_tEvent("" + cCommcmds[pos], Commcmds[pos], null);
    } 
    else if (team=='A' && pos<10){
      send_to_basestation(cCTeamcmds[pos]);//<8
      scoreClients.update_tEvent("" + cCTeamcmds[pos], Teamcmds[pos], teamA.longName);
      mslRemote.update_tEvent("" + cCTeamcmds[pos], Teamcmds[pos], teamA);
    }
    else if (team=='B' && pos<10)
    {
      send_to_basestation(cMTeamcmds[pos]);//<8
      scoreClients.update_tEvent("" + cMTeamcmds[pos], Teamcmds[pos], teamB.longName);
      mslRemote.update_tEvent("" + cMTeamcmds[pos], Teamcmds[pos], teamB);
    }
  }
}

public static void test_send_direct(char team, int pos) {
  if (team=='C') BaseStationServer.write(cCommcmds[pos]);
  if (team=='A') BaseStationServer.write(cCTeamcmds[pos]);
  if (team=='B') BaseStationServer.write(cMTeamcmds[pos]);
}

public static boolean setteamfromip(String s) {
  String clientipstr="127.0.0.*";
  String[] iptokens;
  
  if (!s.equals("0:0:0:0:0:0:0:1")) {
    iptokens=split(s,'.');
    if (iptokens!=null) clientipstr=iptokens[0]+"."+iptokens[1]+"."+iptokens[2]+".*";
  }
  
  //println("Client IP: " + clientipstr);
  
  for (TableRow row : teamstable.rows()) {
    String saddr = row.getString("UnicastAddr");
    if (saddr.equals(clientipstr)) {
      println("Team " + row.getString("Team") + " connected (" + row.getString("shortname8") + "/" + row.getString("longame24") + ")");
      teamselect=row;
      
      boolean noTeamA = teamA.connectedClient == null || !teamA.connectedClient.active();
      boolean noTeamB = teamB.connectedClient == null || !teamB.connectedClient.active();
      
      if(StateMachine.GetCurrentGameState() == GameStateEnum.GS_PREGAME || (noTeamA || noTeamB)) // In pre-game or if lost all connections, ask for the color
      {
        Popup.show(PopupTypeEnum.POPUP_TEAMSELECTION, "Team: "+row.getString("Team")+"\nSelect color or press ESC to cancel","cyan","magenta");
        return true;
      }
      else
      {
        Log.logMessage("ERR No more connections allowed (Attempt from " + s + ")");
        return false;
      }
    }
  }
  Log.logMessage("ERR Unknown team (Attempt from " + s + ")");
  return false;
}

public static void checkBasestationsMessages()
{
  try
  {
    // Get the next available client
    Client thisClient = BaseStationServer.available();
    // If the client is not null, and says something, display what it said
    if (thisClient !=null) {
      
    Team t = null;
    int team = -1; // 0=A, 1=B
      if(teamA != null && teamA.connectedClient == thisClient)
        t=teamA;
      else if(teamB != null && teamB.connectedClient == thisClient)
        t=teamB;
      else{
        if(thisClient != connectingClient)
          println("NON TEAM MESSAGE RECEIVED FROM " + thisClient.ip());
        return;
      }
    String whatClientSaid = new String(thisClient.readBytes());
    if (whatClientSaid != null) 
      while(whatClientSaid.length() !=0){
        //println(whatClientSaid);
        int idx = whatClientSaid.indexOf('\0');
        //println(whatClientSaid.length()+"\t"+ idx);
        if(idx!=-1){
          if(idx!=0)
          {  
            t.wsBuffer+= whatClientSaid.substring(0,idx);
            if(idx < whatClientSaid.length())
              whatClientSaid = whatClientSaid.substring(idx+1);
            else
              whatClientSaid = "";
          }else{
            if(whatClientSaid.length() == 1)
              whatClientSaid = "";
            else
              whatClientSaid = whatClientSaid.substring(1);
          }
          
          // JSON Validation
          boolean ok = true;
          int ageMs = 0;
          String dummyFieldString;
          org.json.JSONArray dummyFieldJsonArray;
          try // Check for malformed JSON
          {
            t.worldstate_json = new org.json.JSONObject(t.wsBuffer);
          } catch(JSONException e) {
            String errorMsg = "ERROR malformed JSON (team=" + t.shortName + ") : " + t.wsBuffer;
            println(errorMsg);
            ok = false;
          }
          
          if(ok)
          {
            try // Check for "type" key
            {
              String type = t.worldstate_json.getString("type");
              
              // type must be "worldstate"
              if(!type.equals("worldstate"))
              {
                String errorMsg = "ERROR key \"type\" is not \"worldstate\" (team=" + t.shortName + ") : " + t.wsBuffer;
                println(errorMsg);
                ok = false;
              }
            } catch(JSONException e) {
              String errorMsg = "ERROR missing key \"type\" (team=" + t.shortName + ") : " + t.wsBuffer;
              println(errorMsg);
              ok = false;
            }
          }
          
          if(ok)
          {
            try // Check for "ageMs" key
            {
              ageMs = t.worldstate_json.getInt("ageMs");
            } catch(JSONException e) {
              String errorMsg = "WS-ERROR missing key \"ageMs\" (team=" + t.shortName + ") : " + t.wsBuffer;
              println(errorMsg);
              ok = false;
            }
          }
          
          if(ok)
          {
            try // Check for "teamName" key
            {
              dummyFieldString = t.worldstate_json.getString("teamName");
            } catch(JSONException e) {
              String errorMsg = "WS-ERROR missing key \"teamName\" (team=" + t.shortName + ") : " + t.wsBuffer;
              println(errorMsg);
              ok = false;
            }
          }
          
          if(ok)
          {
            try // Check for "intention" key
            {
              dummyFieldString = t.worldstate_json.getString("intention");
            } catch(JSONException e) {
              String errorMsg = "WS-ERROR missing key \"intention\" (team=" + t.shortName + ") : " + t.wsBuffer;
              println(errorMsg);
              ok = false;
            }
          }
          
          if(ok)
          {
            try // Check for "robots" key
            {
              dummyFieldJsonArray = t.worldstate_json.getJSONArray("robots");
            } catch(JSONException e) {
              String errorMsg = "WS-ERROR key \"robots\" is missing or is not array (team=" + t.shortName + ") : " + t.wsBuffer;
              println(errorMsg);
              ok = false;
            }
          }
          
          if(ok)
          {
            try // Check for "balls" key
            {
              dummyFieldJsonArray = t.worldstate_json.getJSONArray("balls");
            } catch(JSONException e) {
              String errorMsg = "WS-ERROR key \"balls\" is missing or is not array (team=" + t.shortName + ") : " + t.wsBuffer;
              println(errorMsg);
              ok = false;
            }
          }
          
          if(ok)
          {
            try // Check for "obstacles" key
            {
              dummyFieldJsonArray = t.worldstate_json.getJSONArray("obstacles");
            } catch(JSONException e) {
              String errorMsg = "WS-ERROR key \"obstacles\" is missing or is not array (team=" + t.shortName + ") : " + t.wsBuffer;
              println(errorMsg);
              ok = false;
            }
          }
          
          if(ok)
          {
            t.logWorldstate(t.wsBuffer,ageMs);
          }
          t.wsBuffer="";      
          //println("NEW message");
        }else{
          t.wsBuffer+= whatClientSaid;
          break;
        }
        //println("MESSAGE from " + thisClient.ip() + ": " + whatClientSaid);
        
        // Avoid filling RAM with buffering (for example team is not sending the '\0' character)
        if(t.wsBuffer.length() > 100000) {
          t.wsBuffer = "";
          String errorMsg = "ERROR JSON not terminated with '\\0' (team=" + t.shortName + ")";
          println(errorMsg);
        }
      }
      
      
    }
  }catch(Exception e){
  }
}


// -------------------------
// Referee Box Protocol 2015

// default commands
public static final char COMM_STOP = 'S';
public static final char COMM_START = 's';
public static final char COMM_WELCOME = 'W';  //NEW 2015CAMBADA: welcome message
public static final char COMM_RESET = 'Z';  //NEW 2015CAMBADA: Reset Game
public static final char COMM_TESTMODE_ON = 'U';  //NEW 2015CAMBADA: TestMode On
public static final char COMM_TESTMODE_OFF = 'u';  //NEW 2015CAMBADA: TestMode Off

// penalty Commands 
public static final char COMM_YELLOW_CARD_MAGENTA = 'y';  //NEW 2015CAMBADA: @remote
public static final char COMM_YELLOW_CARD_CYAN = 'Y';//NEW 2015CAMBADA: @remote
public static final char COMM_RED_CARD_MAGENTA = 'r';//NEW 2015CAMBADA: @remote
public static final char COMM_RED_CARD_CYAN = 'R';//NEW 2015CAMBADA: @remote
public static final char COMM_DOUBLE_YELLOW_MAGENTA = 'b'; //NEW 2015CAMBADA: exits field
public static final char COMM_DOUBLE_YELLOW_CYAN = 'B'; //NEW 2015CAMBADA:
//public static final char COMM_DOUBLE_YELLOW_IN_MAGENTA = 'j'; //NEW 2015CAMBADA: 
//public static final char COMM_DOUBLE_YELLOW_IN_CYAN = 'J'; //NEW 2015CAMBADA: 


// game flow commands
public static final char COMM_FIRST_HALF = '1';
public static final char COMM_SECOND_HALF = '2';
public static final char COMM_FIRST_HALF_OVERTIME = '3';  //NEW 2015CAMBADA: 
public static final char COMM_SECOND_HALF_OVERTIME = '4';  //NEW 2015CAMBADA: 
public static final char COMM_HALF_TIME = 'h';
public static final char COMM_END_GAME = 'e';    //ends 2nd part, may go into overtime
public static final char COMM_GAMEOVER = 'z';  //NEW 2015CAMBADA: Game Over
public static final char COMM_PARKING = 'L';

// goal status
public static final char COMM_GOAL_MAGENTA = 'a';
public static final char COMM_GOAL_CYAN = 'A';
public static final char COMM_SUBGOAL_MAGENTA = 'd';
public static final char COMM_SUBGOAL_CYAN = 'D';

// game flow commands
public static final char COMM_KICKOFF_MAGENTA = 'k';
public static final char COMM_KICKOFF_CYAN = 'K';
public static final char COMM_FREEKICK_MAGENTA = 'f';
public static final char COMM_FREEKICK_CYAN = 'F';
public static final char COMM_GOALKICK_MAGENTA = 'g';
public static final char COMM_GOALKICK_CYAN = 'G';
public static final char COMM_THROWIN_MAGENTA = 't';
public static final char COMM_THROWIN_CYAN = 'T';
public static final char COMM_CORNER_MAGENTA = 'c';
public static final char COMM_CORNER_CYAN = 'C';
public static final char COMM_PENALTY_MAGENTA = 'p';
public static final char COMM_PENALTY_CYAN = 'P';
public static final char COMM_DROPPED_BALL = 'N';

// repair Commands
public static final char COMM_REPAIR_OUT_MAGENTA = 'o';  //exits field
public static final char COMM_REPAIR_OUT_CYAN = 'O';

//free: fFHlmMnqQvVxX
//------------------------------------------------------

public static StringDict Description;
public void comms_initDescriptionDictionary() {
  Description = new StringDict();
  Description.set("S", "STOP");
  Description.set("s", "START");
  Description.set("N", "Drop Ball");
  Description.set("h", "Halftime");
  Description.set("e", "End Game");
  Description.set("z", "Game Over");
  Description.set("Z", "Reset Game");
  Description.set("W", "Welcome");
  Description.set("U", "Test Mode on");
  Description.set("u", "Test Mode off");
  Description.set("1", "1st half");
  Description.set("2", "2nd half");
  Description.set("3", "Overtime 1st half");
  Description.set("4", "Overtime 2nd half");
  Description.set("L", "Park");
  
  Description.set("K", "CYAN Kickoff");
  Description.set("F", "CYAN Freekick");
  Description.set("G", "CYAN Goalkick");
  Description.set("T", "CYAN Throw In");
  Description.set("C", "CYAN Corner");
  Description.set("P", "CYAN Penalty Kick");
  Description.set("A", "CYAN Goal+");
  Description.set("D", "CYAN Goal-");
  Description.set("O", "CYAN Repair Out");
  Description.set("R", "CYAN Red Card");
  Description.set("Y", "CYAN Yellow Card");
  Description.set("B", "CYAN Double Yellow");

  Description.set("k", "MAGENTA Kickoff");
  Description.set("f", "MAGENTA Freekick");
  Description.set("g", "MAGENTA Goalkick");
  Description.set("t", "MAGENTA Throw In");
  Description.set("c", "MAGENTA Corner");
  Description.set("p", "MAGENTA Penalty Kick");
  Description.set("a", "MAGENTA Goal+");
  Description.set("d", "MAGENTA Goal-");
  Description.set("o", "MAGENTA Repair Out");
  Description.set("r", "MAGENTA Red Card");
  Description.set("y", "MAGENTA Yellow Card");
  Description.set("b", "MAGENTA Double Yellow");
}


static class Config
{
  // Networking
  public static int scoreServerPort = 12345;
  public static int scoreClientsUpdatePeriod_ms = 1000;
  public static StringList scoreServerClients = new StringList();
  public static int remoteServerPort = 54321;
  public static int basestationServerPort = 28097;
  public static boolean remoteControlEnable = false;
  
  // Rules
  public static int repairPenalty_ms = 30000;
  public static int doubleYellowPenalty_ms = 120000;
  public static int setPieceMaxTime_ms = 7000;

  // Appearance
  public static int maxShortName = 8;
  public static int maxLongName = 24;
  public static int robotPlayColor = 0xffE8FFD8;  //white (very light-green)
  public static int robotRepairColor = 0xff24287B;  //blue
  public static int robotYellowCardColor = 0xffFEFF0F;  //yellow  
  public static int robotDoubleYellowCardColor = 0xff707000;  //doubleyellow
  public static int robotRedCardColor = 0xffE03030;  //red
  public static String defaultCyanTeamShortName = "Team";
  public static String defaultCyanTeamLongName = "Cyan";
  public static int defaultCyanTeamColor = 0xff00ffff;
  public static String defaultMagentaTeamShortName = "Team";
  public static String defaultMagentaTeamLongName = "Magenta";
  public static int defaultMagentaTeamColor  = 0xffff00ff;
  
  // Sounds
  public static String sounds_maxTime = "";
  
  public static void Load(PApplet parent, String filename)
  {
    // file should be inside the "data" folder
    filename = parent.dataPath(filename);
    
    // Read json_string from file
    String json_string = null;
    try{
      BufferedReader reader = new BufferedReader(new FileReader(filename));
      String         line = null;
      StringBuilder  stringBuilder = new StringBuilder();
      String         ls = System.getProperty("line.separator");
      
      try {
          while( ( line = reader.readLine() ) != null ) {
              stringBuilder.append( line );
              stringBuilder.append( ls );
          }
  
          json_string = stringBuilder.toString();
      } finally {
          reader.close();
      }
    }catch(IOException e) {
      println("ERROR accessing file: " + e.getMessage());
      json_string = null;
    }
    
    // If json_string could be read correctly
    if(json_string != null)
    {
      org.json.JSONObject json_root = null;
      try // Check for malformed JSON
      {
        json_root = new org.json.JSONObject(json_string);
      } catch(JSONException e) {
        String errorMsg = "ERROR reading config file : malformed JSON";
        println(errorMsg);
        json_root = null;
      }
      
      // If JSON was correctly parsed
      if(json_root != null)
      {
        try // Get settings
        {
          org.json.JSONObject networking = json_root.getJSONObject("networking");
          org.json.JSONObject rules = json_root.getJSONObject("rules");
          org.json.JSONObject appearance = json_root.getJSONObject("appearance");
          org.json.JSONObject sounds = json_root.getJSONObject("sounds");
          
          // ----
          // Networking
          if(networking.has("scoreServerPort"))
            scoreServerPort = networking.getInt("scoreServerPort");
          
          if(networking.has("scoreServerClients"))
          {
            org.json.JSONArray listOfClients = networking.getJSONArray("scoreServerClients");
            for(int i = 0; i < listOfClients.length(); i++)
            {
              scoreServerClients.append(listOfClients.getString(i));
            }
          }
          
          if(networking.has("remoteServerPort"))
            remoteServerPort = networking.getInt("remoteServerPort");
            
          if(networking.has("basestationServerPort"))
            basestationServerPort = networking.getInt("basestationServerPort");
            
          if(networking.has("scoreClientsUpdatePeriod_ms"))
            scoreClientsUpdatePeriod_ms = networking.getInt("scoreClientsUpdatePeriod_ms");
            
          if(networking.has("remoteControlEnable"))
            remoteControlEnable = networking.getBoolean("remoteControlEnable");
            
          // ----
          // Rules
          if(rules.has("repairPenalty_ms"))
            repairPenalty_ms = rules.getInt("repairPenalty_ms");
            
          if(rules.has("doubleYellowPenalty_ms"))
            doubleYellowPenalty_ms = rules.getInt("doubleYellowPenalty_ms");
          
          if(rules.has("setPieceMaxTime_ms"))
            setPieceMaxTime_ms = rules.getInt("setPieceMaxTime_ms");
            
          // ----
          // Appearance
          if(appearance.has("maxShortName"))
            maxShortName = appearance.getInt("maxShortName");
          
          if(appearance.has("maxLongName"))
            maxLongName = appearance.getInt("maxLongName");
          
          if(appearance.has("robotPlayColor"))
            robotPlayColor = string2color(appearance.getString("robotPlayColor"));
            
          if(appearance.has("robotRepairColor"))
            robotRepairColor = string2color(appearance.getString("robotRepairColor"));
            
          if(appearance.has("robotYellowCardColor"))
            robotYellowCardColor = string2color(appearance.getString("robotYellowCardColor"));
            
          if(appearance.has("robotDoubleYellowCardColor"))
            robotDoubleYellowCardColor = string2color(appearance.getString("robotDoubleYellowCardColor"));
            
          if(appearance.has("robotRedCardColor"))
            robotRedCardColor = string2color(appearance.getString("robotRedCardColor"));
            
          if(appearance.has("defaultCyanTeamShortName"))
            defaultCyanTeamShortName = appearance.getString("defaultCyanTeamShortName");

          if(appearance.has("defaultCyanTeamLongName"))
            defaultCyanTeamLongName = appearance.getString("defaultCyanTeamLongName");
          
          if(appearance.has("defaultCyanTeamColor"))
            defaultCyanTeamColor = string2color(appearance.getString("defaultCyanTeamColor"));

            
          if(appearance.has("defaultMagentaTeamShortName"))
            defaultMagentaTeamShortName = appearance.getString("defaultMagentaTeamShortName");

          if(appearance.has("defaultMagentaTeamLongName"))
            defaultMagentaTeamLongName = appearance.getString("defaultMagentaTeamLongName");
          
          if(appearance.has("defaultMagentaTeamColor"))
            defaultMagentaTeamColor = string2color(appearance.getString("defaultMagentaTeamColor"));
            
          // ----
          // Sounds
          if(sounds.has("maxSetPieceTime"))
            sounds_maxTime = sounds.getString("maxSetPieceTime");
            
        } catch(JSONException e) {
          String errorMsg = "ERROR reading config file...";
          println(errorMsg);
        }
        
      }
    }
    
    if (scoreClientsUpdatePeriod_ms<50) scoreClientsUpdatePeriod_ms=50;
    
    //printConfig();
  }
  
  public static void printConfig()
  {
    // Networking
    println( "### Networking ###" );
    println( "scoreServerPort              : " + scoreServerPort);
    println( "remoteServerPort             : " + remoteServerPort);
    println( "basestationServerPort        : " + basestationServerPort);
    println( "scoreClientsUpdatePeriod_ms  : " + scoreClientsUpdatePeriod_ms);
    println( "remoteControlEnable          : " + remoteControlEnable);
    println();
    // Rules
    println( "### Rules ###" );
    println( "repairPenalty_ms             : " + repairPenalty_ms);
    println( "doubleYellowPenalty_ms       : " + doubleYellowPenalty_ms);
    println();
    // Appearance
    println( "### Appearance ###" );
    println( "maxShortName                 : " + maxShortName);
    println( "maxLongName                  : " + maxLongName);
    println( "robotPlayColor               : " + color2string(robotPlayColor));
    println( "robotRepairColor             : " + color2string(robotRepairColor));
    println( "robotYellowCardColor         : " + color2string(robotYellowCardColor));  
    println( "robotDoubleYellowCardColor   : " + color2string(robotDoubleYellowCardColor));
    println( "robotRedCardColor            : " + color2string(robotRedCardColor));
    println( "defaultCyanTeamShortName     : " + defaultCyanTeamShortName);
    println( "defaultCyanTeamLongName      : " + defaultCyanTeamLongName);
    println( "defaultCyanTeamColor         : " + color2string( defaultCyanTeamColor));
    println( "defaultMagentaTeamShortName  : " + defaultMagentaTeamShortName );
    println( "defaultMagentaTeamLongName   : " + defaultMagentaTeamLongName );
    println( "defaultMagentaTeamColor      : " + color2string( defaultMagentaTeamColor ));
    // Sounds
    println( "### Sounds ###" );
    println( "sounds_maxTime                 : " + sounds_maxTime);
    
  }
}



static class Log
{
  public static boolean enable = true;
  private static PApplet parent = null;
  private static String currentTimedName = "";
  
  public static void init(PApplet p)
  {
    Log.parent = p;
    createLog();
  }
  
  private static String getTimedName()
  {
    return currentTimedName;
  }
  
  private static String createTimedName()
  {
    return nf(year(),4)+nf(month(),2)+nf(day(),2)+"_"+nf(hour(),2)+nf(minute(),2)+nf(second(),2);
  }
  
  public static void createLog() {
    currentTimedName = createTimedName();
    LogFileName=currentTimedName + ".msl";  
    screenlog("Logfile "+LogFileName);
    screenlog("Logging is "+(Log.enable ? "enabled":"disabled"));
    //println("LOG_FILENAME "+LogFileName);
  }
  
  public static void appendTextToFile(String filename, String text) {
    if(parent == null)
      return;
    
    File f = new File(parent.dataPath("tmp/" + filename));
    if (!f.exists()) {
      createFile(f);
    }
    try {
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
      out.println(text);
      out.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  // Log to screen only
  public static void screenlog(String s){
    for (int i=4; i>0; i--)
      Last5cmds[i]=Last5cmds[i-1];
      
    String newLog = nf(hour(),2)+":"+nf(minute(),2)+":"+nf(second(),2)+" "+s;
    if(newLog.length() > 41)
      newLog = newLog.substring(0,40);
    Last5cmds[0]=newLog;
  }
  
  // Log action to both screen and logfile
  public static void logactions(String c) {
    //year()+month()+hour()+minute()+second()+millis()
    //String s1="["+StateMachine.GetCurrentGameStateString()+"] "+Description.get(c+"");
    String s1=Description.get(c+"");
    String s2=System.currentTimeMillis()+","+gametime+"("+gameruntime+"),"+StateMachine.GetCurrentGameStateString()+","+c+","+Description.get(c+"");
    lastaction=c;
  
    screenlog(s1);
    if (Log.enable) Log.appendTextToFile(LogFileName,s2);
    
  }
  
  // Log message to both screen and logfile
  // This function is never used
  public static void logMessage(String s)
  {
    screenlog(s);  
    if (Log.enable) Log.appendTextToFile(LogFileName,s);
  }
  
  public static void createFile(File f) {
    File parentDir = f.getParentFile();
    try {
      parentDir.mkdirs(); 
      f.createNewFile();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  } 
}



/**
  * Log Merger
  *   Ricardo Dias <ricardodias@ua.pt>
  *
  * This class is responsible for merging worldstate information 
  * that come from the teams during the match into a single file.
  * 
  * Based on the timedName, it will search files from both teams
  * on the "data" directory.
  */

static class LogMerger
{
  
// ---
// Atributes
  private org.json.JSONArray tA = null;
  private org.json.JSONArray tB = null;
  private org.json.JSONArray merged = null;
  private String timedName;
  private String teamAName;
  private String teamBName;
  static final int BUFFER = 2048;
// ---


// ---
// Constructor
  public LogMerger(String timedName)
  {
    this.timedName = timedName;
    
    File tAFile = new File(mainApplet.dataPath("tmp/" + timedName + ".A.msl"));
    File tBFile = new File(mainApplet.dataPath("tmp/" + timedName + ".B.msl"));
    
    tA = parseFile(tAFile);
    tB = parseFile(tBFile);
    merged = new org.json.JSONArray();
  }
// ---


// ---
// Parses a File object into a JSONArray
  private org.json.JSONArray parseFile(File file)
  {
    org.json.JSONArray ret = null;
    try
    {
      BufferedReader br = new BufferedReader(new FileReader(file));
      ret = new org.json.JSONArray(new org.json.JSONTokener(br));
    } catch(Exception e) {
      println("ERROR: Problem with file " + file.getAbsolutePath());
    }
    return ret;
  }
// ---


// ---
// Merges the two arrays into one
  public void merge()
  {
    merged = new org.json.JSONArray();
    try
    {
      if(tA == null && tB != null)          // problem with file from team A, merge = teamB
        merged = tB;
      else if(tB == null && tA != null)     // problem with file from team A, merge = teamB
        merged = tA;
      else if(tA != null && tB != null) {   // normal merge
        println("Merging log files...");  
      
        int sizeA = tA.length();
        int sizeB = tB.length();
        
        int iA = 0;
        int iB = 0;
        int nFrames = 0;
        while(nFrames < sizeA + sizeB)
        {
          org.json.JSONObject selected = null;
          if(iA == sizeA) {                   // no more samples from team A
            selected = tB.getJSONObject(iB);
            iB++;
            teamBName = selected.optString("teamName", teamBName);
          } else if(iB == sizeB) {            // no more samples from team B
            selected = tA.getJSONObject(iA);
            iA++;
            teamAName = selected.optString("teamName", teamAName);
          } else {
            org.json.JSONObject oA = tA.getJSONObject(iA);
            org.json.JSONObject oB = tB.getJSONObject(iB);
            if(oA.getInt("timestamp") < oB.getInt("timestamp"))
            {
              selected = tA.getJSONObject(iA);
              iA++;
              teamAName = selected.optString("teamName", teamAName);
            }else{
              selected = tB.getJSONObject(iB);
              iB++;
              teamBName = selected.optString("teamName", teamBName);
            }
          }
          if(selected != null)
            merged.put(selected);
          
          nFrames++;
          println("Merging log files... ["+ nFrames*100.0f/(sizeA+sizeB) +"%]");
        }
      }
    }catch(Exception e) {
      e.printStackTrace();
      return;
    }
    
    writeMergedFile();
    zipAllFiles();
  }
// ---


// ---
// Write merged file into folder
  private boolean writeMergedFile()
  {
    try
    {
      println("Writing merge to file...");
      FileWriter writer = new FileWriter(new File(mainApplet.dataPath("tmp/" + timedName + ".merged.msl")));
      writer.write(merged.toString());
      writer.close();
      println("DONE!");
    } catch(Exception e) {
      println("ERROR Writing merged log file");
      e.printStackTrace();
      return false;
    }
    return true;
  }
// ---


// ---
// Zip all
  public boolean zipAllFiles()
  {    
    try
    {
      println("Zipping game log files...");
      BufferedInputStream origin = null;
      FileOutputStream dest = new FileOutputStream(mainApplet.dataPath("logs/" + timedName + "." + teamAName + "-" + teamBName + ".zip"));
      ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
      out.setMethod(ZipOutputStream.DEFLATED);
      byte data[] = new byte[BUFFER];
      
      //String[] files = {".msl", ".A.msl", ".B.msl", ".merged.msl"};
      String[] files = {".msl", ".A.msl", ".B.msl"};
      for(int i = 0; i < files.length; i++)
      {
        String fileName = mainApplet.dataPath("tmp/" + timedName + files[i]);
        File f = new File(fileName);
        if(!f.exists() || !f.isFile())
          continue;
        
        println("Adding file " + files[i]);
        FileInputStream fi = new FileInputStream(fileName);
        origin = new BufferedInputStream(fi, BUFFER);
        ZipEntry entry = new ZipEntry(timedName + files[i]);
        out.putNextEntry(entry);
        int count;
        while((count = origin.read(data, 0, BUFFER)) != -1) {
          out.write(data, 0, count);
        }
        origin.close();
      }
      out.close();
      println("DONE! \"" + timedName + "." + teamAName + "-" + teamBName + ".zip\" created");
      
    } catch(Exception e) {
      println("ERROR Zipping log files");
      e.printStackTrace();
      return false;
    }
    return true;
  }
// ---
  
}
class MSLRemote
{
  public MyServer server;
  private String lastCommand = " ";
  
  private static final String GAMESTATUS_PRE_GAME = "Va";
  private static final String GAMESTATUS_PRE_GAME_KICK_OFF_CYAN = "Vb";
  private static final String GAMESTATUS_PRE_GAME_KICK_OFF_MAGENTA = "Vc";
  private static final String GAMESTATUS_GAME_STOP_HALF1 = "Vd";
  private static final String GAMESTATUS_GAME_STOP_HALF2 = "Ve";
  private static final String GAMESTATUS_GAME_ON_HALF1 = "Vf";
  private static final String GAMESTATUS_GAME_ON_HALF2 = "Vg";
  private static final String GAMESTATUS_HALF_TIME = "Vh";
  private static final String GAMESTATUS_HALF_KICK_OFF_CYAN = "Vi";
  private static final String GAMESTATUS_HALF_KICK_OFF_MAGENTA = "Vj";
  private static final String GAMESTATUS_END_GAME = "Vk";
  private static final String GAMESTATUS_SET_PLAY = "Vl";
  
  // Set plays:
  private static final String COMMAND_KICK_OFF_CYAN = "CK";
  private static final String COMMAND_FREEKICK_CYAN = "CF";
  private static final String COMMAND_THROW_IN_CYAN = "CT";
  private static final String COMMAND_GOALKICK_CYAN = "CG";
  private static final String COMMAND_CORNER_CYAN = "CE";
  private static final String COMMAND_PENALTY_CYAN = "CP";
  private static final String COMMAND_SCORE_CYAN = "CL"; // append score 2 digits (e.g. "CL01" for cyan score = 1)
  private static final String COMMAND_YELLOW_CYAN = "CY1"; // robot 1
  private static final String COMMAND_OUT_CYAN = "Co1"; // robot 1
  
  private static final String COMMAND_KICK_OFF_MAGENTA= "MK";
  private static final String COMMAND_FREEKICK_MAGENTA = "MF";
  private static final String COMMAND_THROW_IN_MAGENTA = "MT";
  private static final String COMMAND_GOALKICK_MAGENTA = "MG";
  private static final String COMMAND_CORNER_MAGENTA = "ME";
  private static final String COMMAND_PENALTY_MAGENTA = "MP";
  private static final String COMMAND_SCORE_MAGENTA = "ML"; // append score 2 digits (e.g. "CL01" for cyan score = 1)
  private static final String COMMAND_YELLOW_MAGENTA = "MY1"; // robot 1
  private static final String COMMAND_OUT_MAGENTA = "Mo1"; // robot 1
  
  private static final String COMMAND_DROPBALL = "SD";
  private static final String COMMAND_START = "ST";
  private static final String COMMAND_STOP = "SP";
  private static final String COMMAND_ENDPART = "SG";
  private static final String COMMAND_RESET = "SR";
  
  
  
  public MSLRemote(PApplet parent, int port)
  {
    server = new MyServer(parent, port);
  }
  
  public void setLastCommand(String cmd)
  {
    lastCommand = cmd;
  }
  
  public String getEventCommand()
  {
    if(lastCommand.equals(cCommcmds[CMDID_COMMON_START]))
      return COMMAND_START;
    else if(lastCommand.equals(cCommcmds[CMDID_COMMON_STOP]))
      return COMMAND_STOP;
    else if(lastCommand.equals(cCommcmds[CMDID_COMMON_DROP_BALL]))
      return COMMAND_DROPBALL;
    else if(lastCommand.equals(cCommcmds[CMDID_COMMON_HALFTIME]))
      return COMMAND_ENDPART;
    else if(lastCommand.equals(cCommcmds[CMDID_COMMON_RESET]))
      return COMMAND_RESET;
      
    else if(lastCommand.equals(cCTeamcmds[CMDID_TEAM_KICKOFF]))
      return COMMAND_KICK_OFF_CYAN;
    else if(lastCommand.equals(cCTeamcmds[CMDID_TEAM_FREEKICK]))
      return COMMAND_FREEKICK_CYAN;
    else if(lastCommand.equals(cCTeamcmds[CMDID_TEAM_GOALKICK]))
      return COMMAND_GOALKICK_CYAN;
    else if(lastCommand.equals(cCTeamcmds[CMDID_TEAM_THROWIN]))
      return COMMAND_THROW_IN_CYAN;
    else if(lastCommand.equals(cCTeamcmds[CMDID_TEAM_CORNER]))
      return COMMAND_CORNER_CYAN;
    else if(lastCommand.equals(cCTeamcmds[CMDID_TEAM_PENALTY]))
      return COMMAND_PENALTY_CYAN;
    
    else if(lastCommand.equals(cMTeamcmds[CMDID_TEAM_KICKOFF]))
      return COMMAND_KICK_OFF_MAGENTA;
    else if(lastCommand.equals(cMTeamcmds[CMDID_TEAM_FREEKICK]))
      return COMMAND_FREEKICK_MAGENTA;
    else if(lastCommand.equals(cMTeamcmds[CMDID_TEAM_GOALKICK]))
      return COMMAND_GOALKICK_MAGENTA;
    else if(lastCommand.equals(cMTeamcmds[CMDID_TEAM_THROWIN]))
      return COMMAND_THROW_IN_MAGENTA;
    else if(lastCommand.equals(cMTeamcmds[CMDID_TEAM_CORNER]))
      return COMMAND_CORNER_MAGENTA;
    else if(lastCommand.equals(cMTeamcmds[CMDID_TEAM_PENALTY]))
      return COMMAND_PENALTY_MAGENTA;
    
    return "";
  }
  
  public String getGameStatusCommand()
  {    
    // 0  "Pre-Game",
    // 1  "Game - 1st Half",
    // 2  "Game - Halftime",
    // 3  "Game - 2nd Half", 
    // 4  "Game - End", 
    // 5  "Overtime - 1st",
    // 6  "Overtime - Switch",
    // 7  "Overtime - 2nd",
    // 8  "Penalty",
    // 9  "GameOver"
    
    GameStateEnum gs = StateMachine.GetCurrentGameState(); 
    
    boolean kickoff = false;
    boolean teamCyan = false;
    
    if(lastCommand == cCTeamcmds[CMDID_TEAM_KICKOFF] || lastCommand == cMTeamcmds[CMDID_TEAM_KICKOFF])
    {
      if(gs == GameStateEnum.GS_PREGAME || gs == GameStateEnum.GS_HALFTIME || gs == GameStateEnum.GS_HALFTIME_OVERTIME) // pre or halftime
      {
        kickoff = true;
        if(lastCommand == cCTeamcmds[CMDID_TEAM_KICKOFF])
          teamCyan = true;
      }
    }else if(lastCommand == cCTeamcmds[CMDID_TEAM_FREEKICK]
      || lastCommand == cCTeamcmds[CMDID_TEAM_GOALKICK]
      || lastCommand == cCTeamcmds[CMDID_TEAM_THROWIN]
      || lastCommand == cCTeamcmds[CMDID_TEAM_CORNER]
      || lastCommand == cCTeamcmds[CMDID_TEAM_PENALTY]
      || lastCommand == cMTeamcmds[CMDID_TEAM_FREEKICK]
      || lastCommand == cMTeamcmds[CMDID_TEAM_GOALKICK]
      || lastCommand == cMTeamcmds[CMDID_TEAM_THROWIN]
      || lastCommand == cMTeamcmds[CMDID_TEAM_CORNER]
      || lastCommand == cMTeamcmds[CMDID_TEAM_PENALTY]
      || lastCommand == cCommcmds[CMDID_COMMON_DROP_BALL])
        return GAMESTATUS_SET_PLAY;

    switch(gs)
    {
      case GS_PREGAME:
        if(kickoff)
        {
          if(teamCyan)
            return GAMESTATUS_PRE_GAME_KICK_OFF_CYAN;
          else
            return GAMESTATUS_PRE_GAME_KICK_OFF_MAGENTA;
        }
        return GAMESTATUS_PRE_GAME;
      
      
      case GS_GAMESTOP_H1:
      case GS_GAMESTOP_H3:
        return GAMESTATUS_GAME_STOP_HALF1;
      
      case GS_GAMEON_H1:
      case GS_GAMEON_H3:
        return GAMESTATUS_GAME_ON_HALF1;
  
      case GS_HALFTIME:
      case GS_OVERTIME:
      case GS_HALFTIME_OVERTIME:
        if(kickoff)
        {
          if(teamCyan)
            return GAMESTATUS_HALF_KICK_OFF_CYAN;
          else
            return GAMESTATUS_HALF_KICK_OFF_MAGENTA;
        }
        return GAMESTATUS_HALF_TIME;
      
      case GS_GAMESTOP_H2:
      case GS_GAMESTOP_H4:
        return GAMESTATUS_GAME_STOP_HALF2;
      
      case GS_GAMEON_H2:
      case GS_GAMEON_H4:
        return GAMESTATUS_GAME_ON_HALF2;
  
      case GS_PENALTIES:
        return GAMESTATUS_GAME_STOP_HALF2;
      
      case GS_ENDGAME:
        return GAMESTATUS_END_GAME;
    }
    
    return "";
  }
  
  // Sends an "event" type update message to the clients
  public void update_tEvent(String eventCode, String eventDesc, Team team)
  {
    int teamId = -1;
    if(team == teamA) teamId = 0;
    else if(team == teamB) teamId = 1;
    
    int scoreA = (teamA != null) ? teamA.Score : 0;
    int scoreB = (teamB != null) ? teamB.Score : 0;
    
    String msg = "{";
    msg += "\"type\": \"event\",";
    msg += "\"eventCode\": \"" + eventCode + "\",";
    msg += "\"eventDesc\": \"" + eventDesc + "\",";
    msg += "\"teamId\": " + teamId + ",";
    msg += "\"teamName\": \"" + ((teamId == -1) ? "" : team.shortName) + "\",";
    msg += "\"gamestatus\": \"" + getGameStatusCommand() + "\",";
    msg += "\"command\": \"" + getEventCommand() + "\",";
    msg += "\"scoreTeamA\": " + scoreA + ",";
    msg += "\"scoreTeamB\": " + scoreB;
    msg += "}";
    msg += (char)0x00;
    
    //println(msg);
    
    writeMsg(msg);
  }
  
  
  
  public int clientCount()
  {
    return server.clientCount;
  }
  
  public void stopServer()
  {
    server.stop();
  }
  
  public void writeMsg(String message)
  {
    if (server.clientCount > 0){
      server.write(message);
    }
  }
  
  public void checkMessages()
  {
    try
    {
      // Get the next available client
      Client thisClient = server.available();
      // If the client is not null, and says something, display what it said
      if (thisClient !=null) {
        String whatClientSaid = thisClient.readString();
        if (whatClientSaid != null) {
          
          println("MSL Remote JSON: " + whatClientSaid);
          
          org.json.JSONObject jsonObj = new org.json.JSONObject(whatClientSaid);
          
          int pos = jsonObj.getInt("id");
          
          char group = 'C';
          
          buttonEvent(group, pos);
          
        }
      }
    
    }catch(Exception e){
      println("Invalid JSON received from MSL Remote.");
    }
  }
}

public static void resetStartTime(boolean resetSplitTime) {
  tstartTime = System.currentTimeMillis();
  tsplitTime = tstartTime;
  if (resetSplitTime) tprevsplitTime = 0;
  updateScoreClientslasttime = 0;
}

public static long getGameTime() {                    // Used in mslrb2005, Team 
  return (System.currentTimeMillis()-tstartTime);
}

public static long getAbsoluteTime() {                // Used in Comms, 
  return (System.currentTimeMillis());
}

public static long getSplitTime() {                   // Used in mslrb2005, Team
  if (stopsplittimer) return (tprevsplitTime);
  else return ( tprevsplitTime + (System.currentTimeMillis()-tsplitTime) );
}

public static void stopSplitTimer() {                // Used in StateMachine
  if(!stopsplittimer)
  {
    tprevsplitTime+=(System.currentTimeMillis()-tsplitTime);
    stopsplittimer=true;
  }
}

public static void resumeSplitTimer() {              // Used in StateMachine
  if(stopsplittimer)
  {
    tsplitTime = System.currentTimeMillis();
    stopsplittimer=false;
  }
}

// --------------------------------------------------

public void setbackground() {
  rectMode(CENTER);
  textAlign(CENTER, CENTER);

  background(48);

  //center rect
  fill(0, 32);
  stroke(255, 32);
  //rect(400, 288, 256, 208, 16);
  fill(0,16);
  rect(width/2, height/2+28, 256, 300, 16);

  // dividers
  int ramp=34;
  float offsetx=0.35f*width-ramp;
  float offsety=112;
  float m=0.3f;

  //top cyan
  strokeWeight(2);
  fill(Config.defaultCyanTeamColor); 
  stroke(0);
  beginShape();
  vertex(0, 0);
  vertex(0, offsety);
  vertex(offsetx, offsety);
  vertex(offsetx, 0);
  endShape();
  
  //top magenta
  strokeWeight(2);
  fill(Config.defaultMagentaTeamColor); 
  stroke(0);
  beginShape();
  vertex(width, 0);
  vertex(width, offsety);
  vertex(width-offsetx, offsety);
  vertex(width-offsetx, 0);
  endShape();


  //top fill
   fill(96);
   beginShape();
   vertex(offsetx+2, 0);
   vertex(offsetx+2, offsety);
   vertex(offsetx+m*ramp+2, offsety+ramp-1);
   vertex(width-1-offsetx-m*ramp-1, offsety+ramp-1);
   vertex(width-1-offsetx-1, offsety);
   vertex(width-1-offsetx-1, 0);
   endShape();
   

  //bottom
  strokeWeight(2);
  fill(96);
  stroke(0);
  offsety=height-1-128+48;
  beginShape();
  vertex(1, height-2);
  vertex(1, offsety);
  vertex(offsetx, offsety);
  vertex(offsetx+m*ramp, offsety-ramp);
  vertex(width-1-offsetx-m*ramp, offsety-ramp);
  vertex(width-1-offsetx, offsety);
  vertex(width-2, offsety);
  vertex(width-2, height-2);
  vertex(1, height-2);
  endShape();
  
  //bottom fill
   fill(96);
   beginShape();
   vertex(offsetx+2,height);
   vertex(offsetx+2,offsety+1);
   vertex(offsetx+m*ramp+2,offsety-ramp+2);
   vertex(width-1-offsetx-m*ramp-1,offsety-ramp+2);
   vertex(width-1-offsetx-1,offsety+1);
   vertex(width-1-offsetx-1,height);
   endShape();
   
  //carbon
  stroke(0,128);
  for(int i=0; i<width*2; i+=4)
    line(0,i,i,0);
   
  backgroundImage=get();
}

public static int string2color(String hex_string)
{
  int col = 0;
  if (trim(hex_string).charAt(0)=='#')  col=unhex("FF"+trim(hex_string).substring(1));
  return col;
}

public static String color2string(int col)
{
  String ret;
  ret = "" + hex(col);
  ret = "#" + ret.substring(2);
  return ret;
}
static class Popup
{
  private static boolean enabled = false;
  private static PopupTypeEnum type;
  private static boolean newResponse = false;
  private static String lastResponse = "";
  
  private static String message = "";
  private static String btnLeft = "";
  private static String btnRight = "";

  // Methods
  public static boolean isEnabled() { return enabled; }
  public static boolean hasNewResponse() {
    boolean resp = newResponse;
    newResponse = false;  
    return resp;
  }
  public static String getResponse() { return lastResponse; }
  public static PopupTypeEnum getType() { return type; }
  
  public static void show(PopupTypeEnum type, String message, String btnLeft, String btnRight) {
    Popup.type = type;
    Popup.message = message;
    Popup.btnLeft = btnLeft;
    Popup.btnRight = btnRight;
    
    bPopup[0].Label = btnLeft;
    bPopup[1].Label = btnRight;
    bPopup[0].enable();
    bPopup[1].enable();
    enabled = true;
    mainApplet.redraw();
  }
  
  public static void close()
  {
    bPopup[0].disable();
    bPopup[1].disable();
    enabled = false;
    mainApplet.redraw();
    
    // If connectingClient is still referencing a client when closing popup, we have to close the connection
    if(connectingClient != null){
      connectingClient.stop();
      connectingClient = null;
    }
  }
  
  public static void check(boolean mousePress) {
    // check mouse over
    bPopup[0].checkhover();
    bPopup[1].checkhover();
    
    if(mousePress)
    {
      if (bPopup[0].HOVER == true) bPopup[0].activate(); //yes
      if (bPopup[1].HOVER == true) bPopup[1].activate(); //no
      if (bPopup[0].isActive()) {
        lastResponse = btnLeft;
        newResponse = true;
      }
      if (bPopup[1].isActive()) {
        lastResponse = btnRight;
        newResponse = true;
      }
    }
  }
  
  public static void draw() {
    mainApplet.rectMode(CENTER);
    bPopup[0].setxy(mainApplet.width/2-90, mainApplet.height/2+40);
    bPopup[1].setxy(mainApplet.width/2+90, mainApplet.height/2+40);
    mainApplet.fill(0, 160); mainApplet.noStroke();//,224
    mainApplet.rect(mainApplet.width/2, mainApplet.height/2, mainApplet.width, mainApplet.height);
    mainApplet.fill(208); mainApplet.stroke(255);
    mainApplet.rect(mainApplet.width/2, mainApplet.height/2, 400, 200, 8);
    
    mainApplet.fill(64);
    mainApplet.textFont(panelFont);
    mainApplet.textAlign(CENTER, CENTER);
    mainApplet.text( message, mainApplet.width/2, mainApplet.height/2-50);
    bPopup[0].checkhover();
    bPopup[1].checkhover();
    bPopup[0].update();
    bPopup[1].update();
  }
}
public void RefreshButonStatus1() {
  
  switch(StateMachine.GetCurrentGameState())
  {
    // PRE-GAME
    case GS_PREGAME:
    
      buttonAdisableAll();  //team A commands
      buttonBdisableAll();  //team B commands
      buttonCdisable();     //common commands
      
      if(StateMachine.setpiece)
      {
        if(StateMachine.setpiece_cyan)
        {
          buttonFromEnum(ButtonsEnum.BTN_C_KICKOFF).activate();
          buttonFromEnum(ButtonsEnum.BTN_M_KICKOFF).disable();
        }else{
          buttonFromEnum(ButtonsEnum.BTN_C_KICKOFF).disable();
          buttonFromEnum(ButtonsEnum.BTN_M_KICKOFF).activate();
        }
        
        buttonFromEnum(ButtonsEnum.BTN_START).enable();
        buttonFromEnum(ButtonsEnum.BTN_STOP).enable();
      }else{
        buttonFromEnum(ButtonsEnum.BTN_C_KICKOFF).enable();
        buttonFromEnum(ButtonsEnum.BTN_M_KICKOFF).enable();
        
        buttonFromEnum(ButtonsEnum.BTN_START).disable();
        buttonFromEnum(ButtonsEnum.BTN_STOP).activate();
      }
      
      buttonFromEnum(ButtonsEnum.BTN_RESET).enable();

      break;
      
    case GS_GAMEON_H1:
    case GS_GAMEON_H2:
    case GS_GAMEON_H3:
    case GS_GAMEON_H4:
      refreshbutton_game_on();
      break;
    
    case GS_GAMESTOP_H1:
    case GS_GAMESTOP_H2:
    case GS_GAMESTOP_H3:
    case GS_GAMESTOP_H4:
      refreshbutton_game_stopped();
      if(StateMachine.setpiece){
        buttonFromEnum(StateMachine.setpiece_button).activate();
        buttonFromEnum(ButtonsEnum.BTN_START).enable();
      }else{
        buttonFromEnum(ButtonsEnum.BTN_START).disable();
      }
      break;
        
    case GS_HALFTIME:
    case GS_OVERTIME:
    case GS_HALFTIME_OVERTIME:
      buttonAdisableAll();  //team A commands
      buttonBdisableAll();  //team B commands
      buttonCdisable();     //common commands
      
      // Alternate Kick-Offs
      boolean enableCyan = StateMachine.firstKickoffCyan;
      if(StateMachine.GetCurrentGameState() == GameStateEnum.GS_HALFTIME || StateMachine.GetCurrentGameState() == GameStateEnum.GS_HALFTIME_OVERTIME)
        enableCyan = !enableCyan;
      
      if(StateMachine.setpiece)
      {
        buttonFromEnum(StateMachine.setpiece_button).activate();
        buttonFromEnum(ButtonsEnum.BTN_START).enable();
        buttonFromEnum(ButtonsEnum.BTN_STOP).enable();
      }else{
        if(enableCyan)
        {
          buttonFromEnum(ButtonsEnum.BTN_C_KICKOFF).enable();
          buttonFromEnum(ButtonsEnum.BTN_M_KICKOFF).disable();
        }else{
          buttonFromEnum(ButtonsEnum.BTN_C_KICKOFF).disable();
          buttonFromEnum(ButtonsEnum.BTN_M_KICKOFF).enable();
        }
        
        buttonFromEnum(ButtonsEnum.BTN_START).disable();
        buttonFromEnum(ButtonsEnum.BTN_STOP).activate();
        
        if(StateMachine.GetCurrentGameState() == GameStateEnum.GS_OVERTIME)
        {
          bCommoncmds[CMDID_COMMON_RESET].enable();
        }
      }
      
      bCommoncmds[CMDID_COMMON_PARKING].enable();
      
      break;
    
    case GS_PENALTIES:
      refreshbutton_game_stopped();
      buttonAdisable();  //team A commands
      buttonBdisable();  //team B commands
      
      bTeamAcmds[CMDID_TEAM_PENALTY].enable();
      bTeamBcmds[CMDID_TEAM_PENALTY].enable();
      
      if(StateMachine.setpiece)
        buttonFromEnum(StateMachine.setpiece_button).activate();
      buttonFromEnum(ButtonsEnum.BTN_START).enable();
      buttonFromEnum(ButtonsEnum.BTN_STOP).activate();
      
      bCommoncmds[CMDID_COMMON_DROP_BALL].disable();
      bCommoncmds[CMDID_COMMON_HALFTIME].enable();
      bCommoncmds[CMDID_COMMON_PARKING].enable();
      bCommoncmds[CMDID_COMMON_RESET].disable();
      break;
      
    case GS_PENALTIES_ON:
      refreshbutton_game_on();
      break;
      
    case GS_ENDGAME:
      buttonAdisable();  //team A commands
      buttonBdisable();  //team B commands
      buttonCenable();     //common commands
      
      bCommoncmds[CMDID_COMMON_DROP_BALL].disable();
      bCommoncmds[CMDID_COMMON_HALFTIME].disable();
      buttonCSTARTdisable();
      buttonCSTOPactivate();
      break;
    
    default:
      buttonAenable();  //team A commands
      buttonBenable();  //team B commands
      buttonCenable();     //common commands 
      buttonCSTOPactivate();
      break;
      
  }
  
  // The switches are enabled only on pre-game
  if(StateMachine.GetCurrentGameState() != GameStateEnum.GS_PREGAME)
  {
    for(int i = 0; i < bSlider.length; i++)
      bSlider[i].disable();
  }else{
    for(int i = 0; i < bSlider.length; i++)
      bSlider[i].enable();
  }
  
  // Update End Part / End Game button
  String endPartOrEndGame = "End Part";
  switch(StateMachine.GetCurrentGameState())
  {
  case GS_HALFTIME:
  case GS_GAMEON_H2: 
  case GS_GAMEON_H4:
  case GS_GAMESTOP_H2:
  case GS_GAMESTOP_H4:
    endPartOrEndGame = "End Game";
  }
  bCommoncmds[CMDID_COMMON_HALFTIME].Label = endPartOrEndGame; 
}


public void refreshbutton_game_on()
{
  buttonAdisableAll();  //team A commands
  buttonBdisableAll();  //team B commands
  buttonCdisable();     //common commands 
  buttonCSTARTdisable();
  buttonCSTOPactivate();
}

public void refreshbutton_game_stopped()
{

 if(bTeamAcmds[CMDID_TEAM_GOAL].isActive()) {
//    println("Goal Cyan is ON");
    buttonAdisable();
    buttonBdisable();
    buttonCdisable();    
    bTeamBcmds[CMDID_TEAM_KICKOFF].enable();
    bTeamBcmds[CMDID_TEAM_KICKOFF].enable();
    bTeamBcmds[CMDID_TEAM_GOAL].disable();  
    bCommoncmds[CMDID_COMMON_HALFTIME].enable(); 
  }
  else if(bTeamBcmds[CMDID_TEAM_GOAL].isActive()) {
//    println("Goal Magenta is ON");
    buttonAdisable();
    buttonBdisable();
    buttonCdisable();    
    bTeamAcmds[CMDID_TEAM_KICKOFF].enable();    
    bTeamAcmds[CMDID_TEAM_GOAL].disable();    
    bCommoncmds[CMDID_COMMON_HALFTIME].enable();
  }
  else {
    buttonA_setpieces_en();  //team A commands
    buttonB_setpieces_en();  //team B commands

    bCommoncmds[CMDID_COMMON_DROP_BALL].enable();
    bCommoncmds[CMDID_COMMON_HALFTIME].enable(); 
    bCommoncmds[CMDID_COMMON_PARKING].disable();
    bCommoncmds[CMDID_COMMON_RESET].disable();  

    for(int i = CMDID_TEAM_GOAL; i <= CMDID_TEAM_YELLOWCARD; i++)
    {
      if(!bTeamAcmds[i].isActive())
        bTeamAcmds[i].enable();
  
      if(!bTeamBcmds[i].isActive())
        bTeamBcmds[i].enable();
    }  
  }
  buttonCSTOPactivate();
  buttonCSTARTdisable();  
}



// ============================

public void buttonA_setpieces_en()
{
  for (int i=CMDID_TEAM_KICKOFF; i <= CMDID_TEAM_PENALTY; i++)
    bTeamAcmds[i].enable();
}

public void buttonB_setpieces_en()
{
  for (int i=CMDID_TEAM_KICKOFF; i <= CMDID_TEAM_PENALTY; i++)
    bTeamBcmds[i].enable();
}

public void buttonAenable() {
  for (int i=0; i<bTeamAcmds.length; i++) {
    if (i>6 && bTeamAcmds[i].isActive()) ; //maintains goals+repair+cards
    else bTeamAcmds[i].enable();
  }
}
public void buttonBenable() {
  for (int i=0; i<bTeamBcmds.length; i++) {
    if (i>6 && bTeamBcmds[i].isActive()) ; //maintains repair+cards
    else bTeamBcmds[i].enable();
  }
}
public void buttonCenable() {
  for (int i=2; i<bCommoncmds.length; i++)
    bCommoncmds[i].enable();
}
public void buttonAdisable() {
  for (int i=0; i <= CMDID_TEAM_PENALTY; i++)
    bTeamAcmds[i].disable();
}
public void buttonBdisable() {
  for (int i=0; i <= CMDID_TEAM_PENALTY; i++)
    bTeamBcmds[i].disable();
}
public void buttonAdisableAll() {
  for (int i=0; i<bTeamAcmds.length; i++)
    bTeamAcmds[i].disable();
}
public void buttonBdisableAll() {
  for (int i=0; i<bTeamBcmds.length; i++)
    bTeamBcmds[i].disable();
}
public void buttonCdisable() {
  for (int i=2; i<bCommoncmds.length; i++)
    bCommoncmds[i].disable();
}
public void buttonABdisableinactive() {
  for (int i=0; i<bTeamAcmds.length; i++) {
    if (!bTeamAcmds[i].isActive()) bTeamAcmds[i].disable();
    if (!bTeamBcmds[i].isActive()) bTeamBcmds[i].disable();
  }
}
public void buttonABdisablemain() {
  for (int i=0; i < CMDID_TEAM_PENALTY; i++) {
    bTeamAcmds[i].disable();
    bTeamBcmds[i].disable();
  }
}
public void buttonCSTARTenable() {
  bCommoncmds[0].enable();
}
public void buttonCSTARTdisable() {
  bCommoncmds[0].disable();
}
public void buttonCSTOPenable() {
  bCommoncmds[1].enable();
}
public void buttonCSTOPactivate() {
  bCommoncmds[1].activate();
}
public boolean isCSTOPactive() {
  return bCommoncmds[1].isActive();
}
public boolean isCSTARTenabled() {
  return bCommoncmds[0].isEnabled();
}
//==============================================================================
//==============================================================================
class Robot {
  float guix, guiy;
  String state="play"; //play , repair , yellow, doubleyellow , red
  int waittime=-1;
  long DoubleYellowOut=0; 
  long DoubleYellowOutRemain=0; 

  Robot(float zx, float zy) {
    guix=zx; 
    guiy=zy;
  }

//-------------------------------
  public void reset_to_play() {
    state="play";
    waittime=-1;
  }
  
//-------------------------------
  public void reset() {
    state="play";
    waittime=-1;
    this.DoubleYellowOut=0;
    this.DoubleYellowOutRemain=0; 
  }
  
//-------------------------------
  public void setRstate(Robot r) {
    this.state=r.state;
    this.waittime=r.waittime;
    this.DoubleYellowOut=r.DoubleYellowOut;
  }
  
//-------------------------------
  public void updateUI(int c, boolean UIleft) {
    stroke(c); 
    strokeWeight(3);
    int rcolor=255;
    if (this.state.equals("repair")) rcolor=Config.robotRepairColor;
    if (this.state.equals("yellow")) rcolor=Config.robotYellowCardColor;  //yellow  
    if (this.state.equals("doubleyellow")) rcolor=Config.robotDoubleYellowCardColor;  //doubleyellow  
    if (this.state.equals("play")) rcolor=Config.robotPlayColor;  //white (very light-green)
    if (this.state.equals("red")) rcolor=Config.robotRedCardColor;  //red
    fill(rcolor);
    float tx=offsetRight.x + 106 + this.guix;
    float ty=offsetLeft.y + this.guiy;
    if (UIleft) tx=offsetLeft.x - 165 + this.guix;       
    ellipse(tx, ty, 42, 42);  
    fill(255);
    if (waittime>=0)  text(nf(waittime+1, 2), tx, ty);
  }
  
}
//==============================================================================
//==============================================================================
class ScoreClients
{
  public MyServer scoreServer;
  private static final boolean debug = false;
  
  public ScoreClients(PApplet parent, int port)
  {
    scoreServer = new MyServer(parent, port);
  }
  
  // Sends an "event" type update message to the clients
  public void update_tEvent(String eventCode, String eventDesc, String team)
  {
    String msg = "{";
    msg += "\"type\": \"event\",";
    msg += "\"eventCode\": \"" + eventCode + "\",";
    msg += "\"eventDesc\": \"" + eventDesc + "\",";
    msg += "\"team\": \"" + team + "\"";
    msg += "}";
    msg += (char)0x00;
    
    if(debug)
    {
      println("Updating clients: " + eventCode + " (" + eventDesc + ")");
    }
    
    writeMsg(msg);
  }
  
  // Sends a "teams" type update message to the clients
  public void update_tTeams(String gamet,String gamerunt) {
    long startTime = System.currentTimeMillis();
    
    String snA=teamA.shortName;
    String lnA=teamA.longName;
    if (snA.length()>Config.maxShortName) snA=teamA.shortName.substring(0, Config.maxShortName);
    if (lnA.length()>Config.maxLongName) lnA=teamA.longName.substring(0, Config.maxLongName);     
    String snB=teamB.shortName;
    String lnB=teamB.longName;
    if (snB.length()>Config.maxShortName) snB=teamB.shortName.substring(0, Config.maxShortName);     
    if (lnB.length()>Config.maxLongName) lnB=teamB.longName.substring(0, Config.maxLongName);     

    String gamestateText = StateMachine.GetCurrentGameStateString();
    
    String teamA_robotState = "";
    String teamA_robotWaitTime = "";
    String teamA_world_json = "{}";
    if(teamA != null && teamA.worldstate_json != null)
      teamA_world_json = teamA.worldstate_json.toString();
    String teamB_robotState = "";
    String teamB_robotWaitTime = "";
    String teamB_world_json = "{}";
    if(teamB != null && teamB.worldstate_json != null)
      teamB_world_json = teamB.worldstate_json.toString();
    
    for(int i = 0; i < 5; i++){
      teamA_robotState += "\"" + teamA.r[i].state + "\"" + ((i==4)?"":",");
      teamA_robotWaitTime += teamA.r[i].waittime + ((i==4)?"":",");
      teamB_robotState += "\"" + teamB.r[i].state + "\"" + ((i==4)?"":",");
      teamB_robotWaitTime += teamB.r[i].waittime + ((i==4)?"":",");
    }
    
    String msg = "{";
    msg += "\"type\": \"teams\",";
    msg += "\"version\": \"" + MSG_VERSION + "\",";
    msg += "\"gameState\": " + StateMachine.GetCurrentGameState().getValue() + ",";
    msg += "\"gameStateString\": \"" + gamestateText + "\",";
    msg += "\"gameTime\": \"" + gamet + "\",";
    msg += "\"gameRunTime\": \"" + gamerunt + "\",";
    
    msg += "\"teamA\": {"; // Team A
    msg += "\"color\": \"" + hex(teamA.c,6) + "\",";
    msg += "\"shortName\": \"" + snA + "\",";
    msg += "\"longName\": \"" + lnA + "\",";
    msg += "\"score\": \"" + teamA.Score + "\",";
    msg += "\"robotState\": [" + teamA_robotState + "],";
    msg += "\"robotWaitTime\": [" + teamA_robotWaitTime + "],";
    msg += "\"worldState\": " + teamA_world_json;
    msg += "},"; // END Team A
    
    msg += "\"teamB\": {"; // Team B
    msg += "\"color\": \"" + hex(teamB.c,6) + "\",";
    msg += "\"shortName\": \"" + snB + "\",";
    msg += "\"longName\": \"" + lnB + "\",";
    msg += "\"score\": \"" + teamB.Score + "\",";
    msg += "\"robotState\": [" + teamB_robotState + "],";
    msg += "\"robotWaitTime\": [" + teamB_robotWaitTime + "],";
    msg += "\"worldState\": " + teamB_world_json;
    msg += "}"; // END Team B
    
    msg += "}";
    
    msg += (char)0x00;
    
    writeMsg(msg);
    updateScoreClientslasttime=System.currentTimeMillis();
    
    //logMessage("Send to score clients " + (updateScoreClientslasttime-startTime) + " ms");
  }
  
  public int clientCount()
  {
    return scoreServer.clientCount;
  }
  
  public void stopServer()
  {
    scoreServer.stop();
  }
  
  public void writeMsg(String message)
  {
    if (scoreServer.clientCount>0){
      scoreServer.write(message);
    }
  }
  
}
static class StateMachine
{
  
  private static boolean needUpdate = false; 
  private static boolean btnOn = false;
  private static ButtonsEnum btnCurrent = ButtonsEnum.BTN_ILLEGAL;
  private static ButtonsEnum btnPrev = ButtonsEnum.BTN_ILLEGAL;
  public static GameStateEnum gsCurrent = GameStateEnum.GS_PREGAME;
  private static GameStateEnum gsPrev = GameStateEnum.GS_ILLEGAL;
  
  public static boolean setpiece = false;
  public static boolean setpiece_cyan = false;
  public static ButtonsEnum setpiece_button = null;
  
  public static boolean firstKickoffCyan = true;

  
  
  public static void Update(ButtonsEnum click_btn, boolean on)
  {
    //println("Updating clicked button: " + click_btn.getValue());
    btnCurrent = click_btn;
    btnOn = on;
    needUpdate = true;
    
    StateMachineRefresh();
  }
  
  //
  // Basic state machine main refresh
  //
  private static void StateMachineRefresh()
  {
    GameStateEnum nextGS = GameStateEnum.newInstance(gsCurrent);
    GameStateEnum saveGS = GameStateEnum.newInstance(gsCurrent);
    
    // Check popup response when popup is ON
    if(Popup.hasNewResponse())
    {
      switch(Popup.getType())
      {
        case POPUP_RESET:
        {
          if(Popup.getResponse().equals("yes"))
          {
            send_event_v2(cCommcmds[CMDID_COMMON_RESET], Commcmds[CMDID_COMMON_RESET], null);
            reset();
          }
          break;
        }
        
        case POPUP_ENDPART:
        {
          if(Popup.getResponse().equals("yes"))
          {
            gsCurrent = SwitchGamePart();
            gsPrev = saveGS;
            resetStartTime(false);
            if (bCommoncmds[CMDID_COMMON_HALFTIME].Label.equals("End Game"))
              send_event_v2(cCommcmds[CMDID_COMMON_ENDGAME], Commcmds[CMDID_COMMON_ENDGAME], null);
            else
              send_event_v2(cCommcmds[CMDID_COMMON_HALFTIME], Commcmds[CMDID_COMMON_HALFTIME], null);            
          }
          break;
        }
        
        case POPUP_TEAMSELECTION:
        {
          Team t = null;
          if(Popup.getResponse().equals("cyan"))
          {
            //println("cyan - " + teamselect.getString("shortname8"));
            Log.logMessage("Connection from " + connectingClient.ip() + " accepted - Cyan");
            t = teamA;
          }else{
            //println("magenta - " + teamselect.getString("shortname8"));
            Log.logMessage("Connection from " + connectingClient.ip() + " accepted - Magenta");
            t = teamB;
          }
          
          if(t != null)
            t.teamConnected(teamselect);
          
          break;
        }
      }
      
      needUpdate = false;
      Popup.close();
      return;
    }
    
    if(needUpdate)
    {
      //println("Updating state machine: btn " + btnCurrent.getValue());
      // Goal buttons
      int add = (btnOn ? +1 : -1);
      if(btnCurrent.isGoal())
      {
        if(btnCurrent.isCyan()) teamA.Score+=add;
        else teamB.Score+=add;
      }
      else if(btnCurrent.isReset())
      {
        Popup.show(PopupTypeEnum.POPUP_RESET, MSG_RESET, "yes", "no");
        needUpdate = false;
        return;
      }
      else if(btnCurrent.isEndPart())
      {
        Popup.show(PopupTypeEnum.POPUP_ENDPART, MSG_HALFTIME, "yes", "no");
        needUpdate = false;
        return;
      }
      else if(btnCurrent.isRepair())
      {
        if(btnCurrent.isCyan())
          teamA.newRepair=btnOn;
        else
          teamB.newRepair=btnOn;
      }
      else if(btnCurrent.isRed())
      {
        if(btnCurrent.isCyan())
          teamA.newRedCard=btnOn;
        else
          teamB.newRedCard=btnOn;
      }
      else if(btnCurrent.isYellow())
      {
        Team t = teamA;
        if(!btnCurrent.isCyan())
          t = teamB;
        
        if (t.YellowCardCount==1)
          t.newDoubleYellow = btnOn;
        else
          t.newYellowCard = btnOn;
      }
      
      switch(gsCurrent)
      {
        // PRE-GAME and Half Times
        case GS_PREGAME:
        case GS_HALFTIME:
        case GS_OVERTIME:
        case GS_HALFTIME_OVERTIME:
          if(btnCurrent == ButtonsEnum.BTN_START)
          {
            resetStartTime(true);
            nextGS = SwitchRunningStopped();
            switch(nextGS)
            {
              case GS_GAMEON_H1: send_to_basestation(COMM_FIRST_HALF + ""); break;
              case GS_GAMEON_H2: send_to_basestation(COMM_SECOND_HALF + ""); break;
              case GS_GAMEON_H3: send_to_basestation(COMM_FIRST_HALF_OVERTIME + ""); break;
              case GS_GAMEON_H4: send_to_basestation(COMM_SECOND_HALF_OVERTIME + ""); break;
            }
          }
          else if(btnCurrent == ButtonsEnum.BTN_STOP)
          {
            if(setpiece)
              ResetSetpiece();
          }
          else if(btnCurrent == ButtonsEnum.BTN_C_KICKOFF)
          {
            // Save first kickoff
            if(gsCurrent == GameStateEnum.GS_PREGAME)
              firstKickoffCyan = true;
            SetSetpiece(true, btnCurrent);
          }else if(btnCurrent == ButtonsEnum.BTN_M_KICKOFF){
            if(gsCurrent == GameStateEnum.GS_PREGAME)
              firstKickoffCyan = false;
            SetSetpiece(false, btnCurrent);
          }
          
          break;
          
        case GS_GAMESTOP_H1:
        case GS_GAMESTOP_H2:
        case GS_GAMESTOP_H3:
        case GS_GAMESTOP_H4:
          if(btnCurrent.isSetPiece())
            SetSetpiece(btnCurrent.isCyan(), btnCurrent);
          else if(btnCurrent.isStart()){
            nextGS = SwitchRunningStopped();
          }
          else if(btnCurrent.isStop())
            ResetSetpiece();
          else if(btnCurrent.isEndPart())
            nextGS = SwitchGamePart();
          
          break;
        
        case GS_GAMEON_H1:
        case GS_GAMEON_H2:
        case GS_GAMEON_H3:
        case GS_GAMEON_H4:
          if(setpiece)
            ResetSetpiece();
            
          if(btnCurrent == ButtonsEnum.BTN_STOP)
          {
            nextGS = SwitchRunningStopped();
          }
            
          break;
          
        case GS_PENALTIES:
          if(btnCurrent.isSetPiece())                        // Kick Off either, Penalty either, DropBall
            SetSetpiece(btnCurrent.isCyan(), btnCurrent);
          else if(btnCurrent.isStop())
            ResetSetpiece();
          else if(btnCurrent.isEndPart())
            nextGS = SwitchGamePart();
          else if(btnCurrent.isStart())
            nextGS = SwitchRunningStopped();
            
          break;
        
        case GS_PENALTIES_ON:
          if(setpiece)
            ResetSetpiece();
          if(btnCurrent.isStop())
            nextGS = SwitchRunningStopped();
          break;
          
        case GS_ENDGAME:
          if(btnCurrent.isEndPart()){
            println("RESETING");
            reset();
          }
          break;
      }
      
      if(nextGS != null)
      {
        // Update split time
        if(nextGS.isRunning())
          resumeSplitTimer();
        else
          stopSplitTimer();
        
        gsCurrent = nextGS;
        gsPrev = saveGS;
        
        //println("gs: " + gsPrev.getValue() + " -> " + nextGS.getValue());
        
        if(gsCurrent.getValue() != gsPrev.getValue())
        {
          teamA.checkflags();
          teamB.checkflags();
        }
      }
      
      btnPrev = btnCurrent;
      
      
      
      needUpdate = false;
    }
  }
  
  private static GameStateEnum SwitchGamePart()
  {
    switch(gsCurrent)
    {
      case GS_GAMESTOP_H1: return GameStateEnum.GS_HALFTIME;
      case GS_GAMESTOP_H2: return GameStateEnum.GS_OVERTIME;
      case GS_GAMESTOP_H3: return GameStateEnum.GS_HALFTIME_OVERTIME;
      case GS_GAMESTOP_H4: return GameStateEnum.GS_PENALTIES;
      case GS_PENALTIES: return GameStateEnum.GS_ENDGAME;
    }
    
    return null;
  }
  
  private static GameStateEnum SwitchRunningStopped()
  {
    switch(gsCurrent)
    {
      case GS_GAMEON_H1: return GameStateEnum.GS_GAMESTOP_H1;
      case GS_GAMEON_H2: return GameStateEnum.GS_GAMESTOP_H2;
      case GS_GAMEON_H3: return GameStateEnum.GS_GAMESTOP_H3;
      case GS_GAMEON_H4: return GameStateEnum.GS_GAMESTOP_H4;
      
      case GS_PREGAME:
      case GS_GAMESTOP_H1:
        return GameStateEnum.GS_GAMEON_H1;
      case GS_HALFTIME:
      case GS_GAMESTOP_H2:
        return GameStateEnum.GS_GAMEON_H2;
      case GS_OVERTIME:
      case GS_GAMESTOP_H3:
        return GameStateEnum.GS_GAMEON_H3;
      case GS_HALFTIME_OVERTIME:
      case GS_GAMESTOP_H4:
        return GameStateEnum.GS_GAMEON_H4;
        
      case GS_PENALTIES: return GameStateEnum.GS_PENALTIES_ON;
      case GS_PENALTIES_ON: return GameStateEnum.GS_PENALTIES;
    }
    
    return null;
  }
  
  private static void ResetSetpiece()
  {
    setpiece = false;
  }
  
  private static void SetSetpiece(boolean cyan, ButtonsEnum btn)
  {
      setpiece = true;
      setpiece_cyan = cyan;
      setpiece_button = btn;
  }
  
  public static GameStateEnum GetCurrentGameState()
  {
    return gsCurrent;
  }
  
  public static String GetCurrentGameStateString()
  {
    if(gsCurrent != null)
      return gsCurrent.getName();
    else
      return "";
  }
  
  public static void reset()
  {
    try {
      send_to_basestation("" + COMM_RESET);
      
      needUpdate = false; 
      btnCurrent = ButtonsEnum.BTN_ILLEGAL;
      btnPrev = ButtonsEnum.BTN_ILLEGAL;
      gsCurrent = GameStateEnum.GS_PREGAME;
      gsPrev = GameStateEnum.GS_ILLEGAL;
      
      teamA.reset();
      teamB.reset();        
      teamA.resetname();
      teamB.resetname();        
      resetStartTime(true);
      
      LogMerger merger = new LogMerger(Log.getTimedName());
      merger.merge();
      
      Log.createLog();
      
      BaseStationServer.stop();
      BaseStationServer = new MyServer(mainApplet, Config.basestationServerPort);
    } catch(Exception e) {}
  }
  
  public static boolean isHalf()
  {
    return is1stHalf() || is2ndHalf() || is3rdHalf() || is4thHalf();
  }

  public static boolean isPreGame()
  {
    return gsCurrent == GameStateEnum.GS_PREGAME;
  }
  
  public static boolean is1stHalf()
  {
    return gsCurrent == GameStateEnum.GS_GAMESTOP_H1 || gsCurrent == GameStateEnum.GS_GAMEON_H1;
  }
  
  public static boolean is2ndHalf()
  {
    return gsCurrent == GameStateEnum.GS_GAMESTOP_H2 || gsCurrent == GameStateEnum.GS_GAMEON_H2;
  }
  
  public static boolean is3rdHalf()
  {
    return gsCurrent == GameStateEnum.GS_GAMESTOP_H3 || gsCurrent == GameStateEnum.GS_GAMEON_H3;
  }
  
  public static boolean is4thHalf()
  {
    return gsCurrent == GameStateEnum.GS_GAMESTOP_H4 || gsCurrent == GameStateEnum.GS_GAMEON_H4;
  }

  public static boolean isInterval() {
    return gsCurrent == GameStateEnum.GS_HALFTIME || gsCurrent == GameStateEnum.GS_OVERTIME || gsCurrent == GameStateEnum.GS_HALFTIME_OVERTIME || gsCurrent == GameStateEnum.GS_GAMESTOP_H4 || gsCurrent == GameStateEnum.GS_PENALTIES;
  }

}

public void StateMachineCheck() {
  StateMachine.StateMachineRefresh();
}
// Processing mouse'event
public void mousePressed() {
  if (!Popup.isEnabled()) {
    //sliders
    boolean refreshslider = false;
    int pos = -1;
    
    for (int i=0; i<4; i++)
      if (bSlider[i].mouseover()) { bSlider[i].toogle(); refreshslider=true; pos=i; break;}    
    if (refreshslider) {
      
    setbooleansfrombsliders();
    //if (pos==0) screenlog("Testmode "+(TESTMODE?"enabled":"disabled"));
    if (pos==1) Log.screenlog("Log "+(Log.enable?"enabled":"disabled"));
    if (pos==2) Log.screenlog("Remote "+(REMOTECONTROLENABLE?"enabled":"disabled"));
    
      
//    RefreshButonStatus();
    }
    
    //common commands
    for (int i=0; i<bCommoncmds.length; i++) {
      if (bCommoncmds[i].isEnabled()) {
        bCommoncmds[i].checkhover();
        if (bCommoncmds[i].HOVER==true) { 
          buttonEvent('C', i); 
          break;
        }
      }
    }
    
    //team commands
    for (int i=0; i<bTeamAcmds.length; i++) {
      if (bTeamAcmds[i].isEnabled()) {
        bTeamAcmds[i].checkhover();
        if (bTeamAcmds[i].HOVER==true) { 
          buttonEvent('A', i); 
          break;
        }
      }
      if (bTeamBcmds[i].isEnabled()) {
        bTeamBcmds[i].checkhover();
        if (bTeamBcmds[i].HOVER==true) { 
          buttonEvent('B', i); 
          break;
        }
      }
    }
      
  }
  else {//POPUP
    Popup.check(true);
  }

  //frameRate(appFrameRate);
  //redraw();
}

// Processing mouse'event
public void mouseMoved() {
  if (!Popup.isEnabled()) {
    for (int i=0; i<bTeamAcmds.length; i++) {
      if (bTeamAcmds[i].isEnabled()) bTeamAcmds[i].checkhover();
      if (bTeamBcmds[i].isEnabled()) bTeamBcmds[i].checkhover();
    }  
    for (int i=0; i<bCommoncmds.length; i++)
      if (bCommoncmds[i].isEnabled()) bCommoncmds[i].checkhover();  
  } 
  else {  //check popup
    Popup.check(false);
  }

  //frameRate(appFrameRate);
  //redraw();
}

// Processing key'event
public void keyPressed() {
  if (key == ESC){
    key = 0; //disable quit on ESC
    
    // Close popup
    if(Popup.isEnabled()) 
      Popup.close();
  }

}


class Team {
  String shortName;  //max 8 chars
  String longName;  //max 24 chars
  String unicastIP, multicastIP;
  int c=(0xff000000);
  boolean isCyan;  //default: cyan@left
  boolean newYellowCard, newRedCard, newRepair, newDoubleYellow, newPenaltyKick, newGoal; // Pending commands, effective only on gamestate change
  int Score, RepairCount, RedCardCount, YellowCardCount, DoubleYellowCardCount, PenaltyCount;
  long RepairOut;
  int tableindex=0;
  org.json.JSONObject worldstate_json;
  String wsBuffer;
  Robot[] r=new Robot[5];
  
  File logFile;
  PrintWriter logFileOut;
  Client connectedClient;
  boolean firstWorldState;
      
  Team(int c, boolean uileftside) {
    this.c=c;
    this.isCyan=uileftside;
    
    //robots
    float x=0, y=64; 
    r[0]=new Robot(x, y);
    r[1]=new Robot(x+56, y);
    r[2]=new Robot(x, y + 56);
    r[3]=new Robot(x+56, y + 56);
    r[4]=new Robot(x+28, y + 112);
    
    this.reset();
  }

  //===================================
 
  public void resetname(){
    if (this.isCyan) {
      this.shortName=Config.defaultCyanTeamShortName;
      this.longName=Config.defaultCyanTeamLongName;
    }
    else {
      this.shortName=Config.defaultMagentaTeamShortName;
      this.longName=Config.defaultMagentaTeamLongName;
    }
  }
  
  public void logWorldstate(String teamWorldstate, int ageMs)
  {
    if(logFileOut == null)
      return;
    
    if(firstWorldState) {
      logFileOut.println("[");    // Start of JSON array
      firstWorldState = false;
    }else{
      logFileOut.println(",");    // Separator for the new JSON object
    }
    
    logFileOut.print("{");
    logFileOut.print("\"teamName\": " + shortName + ",");
    logFileOut.print("\"timestamp\": " + (System.currentTimeMillis() - ageMs) + ",");
    logFileOut.print("\"gametimeMs\": " + getGameTime() + ",");
    logFileOut.print("\"worldstate\": " + teamWorldstate);
    logFileOut.print("}");
    
  }
  
  public void reset() {
    if(logFileOut != null) {
      logFileOut.println("]");    // End JSON array
      logFileOut.close();
    }
    
    logFileOut = null;
    logFile = null;

    this.resetname();
    
    this.worldstate_json = null;
    this.wsBuffer = "";
    
    this.Score=0; 
    this.RepairCount=0;
    this.RedCardCount=0;
    this.YellowCardCount=0;
    this.DoubleYellowCardCount=0;
    this.PenaltyCount=0;
    this.RepairOut=0;
    this.newYellowCard=false;
    this.newRedCard=false;
    this.newRepair=false;
    this.newDoubleYellow=false;
    this.newPenaltyKick=false;
    for (int i=0; i<5; i++)
      r[i].reset();
    
    if(this.connectedClient != null && this.connectedClient.active())
      this.connectedClient.stop();
    this.connectedClient = null;
    
    this.firstWorldState = true;
  }

  // Function called when team connects and is accepted
  public void teamConnected(TableRow teamselect)
  {
    shortName=teamselect.getString("shortname8");
    longName=teamselect.getString("longame24");
    unicastIP = teamselect.getString("UnicastAddr");
    multicastIP = teamselect.getString("MulticastAddr");
    
    
    if(connectedClient != null)
      BaseStationServer.disconnect(connectedClient);
    
    connectedClient = connectingClient;
    connectingClient.write(COMM_WELCOME);
    connectingClient = null;
    
    if(this.logFile == null || this.logFileOut == null)
    {
      this.logFile = new File(mainApplet.dataPath("tmp/" + Log.getTimedName() + "." + (isCyan?"A":"B") + ".msl"));
      try{
        this.logFileOut = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
      }catch(IOException e){ }
    }
  }
    
//*******************************************************************
//*******************************************************************
  public void repair_timer_start() { 
    this.RepairOut=getSplitTime()+Config.repairPenalty_ms;
  }
  
//*******************************************************************
  public void repair_timer_check() {
    long remain=RepairOut-getSplitTime();
    if (StateMachine.isInterval()) {
        remain = -1;
        println("Repair reseted!");
    }
    if (remain>=0)
      for(int i=0; i<RepairCount; i++) r[i].waittime=PApplet.parseInt(remain/1000);
    else {
      for(int i=0; i<RepairCount; i++) r[i].waittime=-1;
      RepairCount=0;
      println("Repair OUT: "+shortName+" @"+(isCyan?"left":"right"));
    }
  }
  
//*******************************************************************
  public void double_yellow_timer_start() {
    r[5-DoubleYellowCardCount].DoubleYellowOut=getAbsoluteTime()+Config.doubleYellowPenalty_ms;
  }

  public void double_yellow_timer_check() {
    for (int i=(5-DoubleYellowCardCount); i<5; i++) {
    long remain;
    if (StateMachine.isHalf()) {
      remain=r[i].DoubleYellowOut-getAbsoluteTime();
    }
    else {
      remain = r[i].waittime * 1000;
      r[i].DoubleYellowOut = remain + getAbsoluteTime();
    }
        
        if (remain>=0)
         r[i].waittime=PApplet.parseInt(remain/1000);
        else {  //shift right & reset
          r[i].reset();
          for (int j=4; j>0; j--) {
            if (!r[j].state.equals("doubleyellow") && r[j-1].state.equals("doubleyellow")){
              r[j].setRstate(r[j-1]);
              r[j-1].reset();
            }
          }
        DoubleYellowCardCount--;
        println("Double Yellow end: "+shortName+" @"+(isCyan?"left":"right"));
      }
                
    }
  }
//*******************************************************************
  public void setDoubleYellowOutRemain() {
    println("setDoubleYellowOutRemain");
    for (int j=0; j<5; j++) {
      if (r[j].state.equals("doubleyellow"))  r[j].DoubleYellowOutRemain=r[j].DoubleYellowOut-getGameTime();
      else r[j].DoubleYellowOutRemain=0;
    }
  }

//*******************************************************************
  public void resumeDoubleYellowOutRemain() {
    println("resumeDoubleYellowOutRemain");
    for (int j=0; j<5; j++) {
      if (r[j].state.equals("doubleyellow"))  r[j].DoubleYellowOut=r[j].DoubleYellowOutRemain;
      r[j].DoubleYellowOutRemain=0;
    }
  }
  
//*******************************************************************
  public void repairclear() {
    this.RepairCount=0;
    this.RepairOut=0;
    for (int i=0; i<5; i++)
      if (r[i].state.equals("repair"))  r[i].reset_to_play();
  }
  
//*******************************************************************
  public void checkflags() {
    if (this.newRepair) {
      this.RepairCount++; 
      this.repair_timer_start();
      this.newRepair=false;

      // Hack: send command only on game change
      if(this.isCyan) event_message_v2(ButtonsEnum.BTN_C_REPAIR, true);
      else event_message_v2(ButtonsEnum.BTN_M_REPAIR, true);
    }
    if (this.newYellowCard) {
      this.YellowCardCount=1;
      this.newYellowCard=false;

      // Hack: send command only on game change
      if(this.isCyan) event_message_v2(ButtonsEnum.BTN_C_YELLOW, true);
      else event_message_v2(ButtonsEnum.BTN_M_YELLOW, true);
    }
    if (this.newRedCard) {
      this.RedCardCount++;
      this.newRedCard=false;

      // Hack: send command only on game change
      if(this.isCyan) event_message_v2(ButtonsEnum.BTN_C_RED, true);
      else event_message_v2(ButtonsEnum.BTN_M_RED, true);
    }
    if (this.newDoubleYellow) {
      this.DoubleYellowCardCount++;
      this.YellowCardCount=0;
      this.double_yellow_timer_start();
      this.newDoubleYellow=false;

      if(this.isCyan) send_event_v2(""+COMM_DOUBLE_YELLOW_CYAN, "Double Yellow", this);
      else send_event_v2(""+COMM_DOUBLE_YELLOW_MAGENTA, "Double Yellow", this);
    }
    if (this.newPenaltyKick) {
      this.PenaltyCount++;
      this.newPenaltyKick=false;
    }
  
  }
  
//*******************************************************************
//*******************************************************************
  
  public void updateUI() {
    if(connectedClient != null && !connectedClient.active())
    {
      println("Connection to team \"" + longName + "\" dropped.");
      Log.logMessage("Team " + shortName + " dropped");
      BaseStationServer.disconnect(connectedClient);
      resetname();
      connectedClient = null;
    }
    
    //team names
    String sn=shortName;
    String ln=longName;
    if (sn.length()>Config.maxShortName) sn=shortName.substring(0, Config.maxShortName);
    if (ln.length()>Config.maxLongName) ln=longName.substring(0, Config.maxLongName);
    rectMode(CENTER);
    fill(255);
    textFont(teamFont);
    textAlign(CENTER, CENTER);    
    if (isCyan) text(sn, 163, 50);
    else text(sn, 837, 50);
    textFont(panelFont);
    if (isCyan) text(ln, 163, 90);
    else text(ln, 837, 90);


    // robot state 
    for (int i=0; i<5; i++) r[i].state="play";//in-game: white, default setting
    for (int i=0; i<RepairCount; i++)  r[i].state="repair";//in-repair: blue
    for (int i=RepairCount; i< min(RepairCount+YellowCardCount, 5); i++)  r[i].state="yellow"; //yellow-card: yellow
    for (int i=(RepairCount+YellowCardCount); i<min(RepairCount+YellowCardCount+RedCardCount, 5); i++)  r[i].state="red";//red
    for (int i=(5-DoubleYellowCardCount); i<5; i++)  r[i].state="doubleyellow";//doubleyellow

    if (RepairCount > 0)            //repair #
      repair_timer_check();
    if (DoubleYellowCardCount > 0)  //double yellow #
      double_yellow_timer_check();
    
    for (int i=0; i<5; i++)
      r[i].updateUI(c,isCyan);

    textAlign(LEFT, BOTTOM);
    textFont(debugFont);
    fill(0xffffff00);
    textLeading(20);
    String ts="Goals."+this.Score+" Penalty:"+this.PenaltyCount+"\nYellow:"+this.YellowCardCount+" Red:"+this.RedCardCount+"\nRepair:"+this.RepairCount+" 2xYellow:"+this.DoubleYellowCardCount;
    if (isCyan) text(ts, 40, height-18);
    else text(ts, width - 190, height-18);
  }
  
//*******************************************************************
  public boolean IPBelongs(String clientipstr)
  {
    if(this.unicastIP == null)
      return false;
    
    String[] iptokens;
    
    if (!clientipstr.equals("0:0:0:0:0:0:0:1")) {
      iptokens=split(clientipstr,'.');
      if (iptokens!=null) clientipstr=iptokens[0]+"."+iptokens[1]+"."+iptokens[2]+".*";
    }
    
    return this.unicastIP.equals(clientipstr);
  }
}
  public void settings() {  size(1000, 680); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "mslrb2015" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
