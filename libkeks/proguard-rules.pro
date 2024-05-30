
-keep interface com.eveningoutpost.dexdrip.plugin.IPluginDA

# entry points need to be in umbilical to avoid being stripped
-keepnames class jamorham.keks.Plugin {
  public byte[][] aNext();
  public boolean receivedData(byte[]);
  public boolean receivedResponse(byte[]);
  public void amConnected();
  public boolean bondNow(byte[]);
  public static jamorham.keks.Plugin getInstance(java.lang.String);
  public byte[][] aNext();
  public byte[][] bNext();
  byte[][] cNext();
  void amConnected();
  boolean bondNow(byte[]);
  boolean receivedResponse(byte[]);
  boolean receivedResponse2(byte[]);
  boolean receivedResponse3(byte[]);
  boolean receivedData(byte[]);
  boolean receivedData2(byte[]);
  boolean receivedData3(byte[]);
  byte[] getPersistence(int);
  boolean setPersistence(int, byte[]);
  java.lang.String getStatus();
  java.lang.String getName();

 }

-keep class jamorham.keks.Umbilical





