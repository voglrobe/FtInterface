/**
 * Arduino Uno firmware to control the Trainings Roboter kit
 * via the orginal Centronics ft-Interface.
 * 
 * 2016, Robert Vogl
 */

// Pin configuration
#define CLK       PD3 // Pin 3
#define DATA_OUT  PD7 // Pin 7
#define LOAD_IN   PD2 // Pin 2
#define DATA_IN   PD5 // Pin 5
#define LOAD_OUT  PB0 // Pin 8
#define TRIGGER_X PD6 // Pin 6
#define TRIGGER_Y PD4 // Pin 4
#define EMERG_OFF PB4 // Pin 12
#define ERROR_PIN 13


// Digital outputs masks for M1-M4
const byte M1 = B11000000;
const byte M2 = B00110000;
const byte M3 = B00001100;
const byte M4 = B00000011;

const byte M1_OFF = ~M1;
const byte M2_OFF = ~M2;
const byte M3_OFF = ~M3;
const byte M4_OFF = ~M4;

struct t_command {
  byte seqnr; // 6 Bit sequence number (default = 0).
  byte mcb; // Motor Control Byte |M1|M2|M3|M4|. 00 = off, 01 = turn right, 10 = turn left, 11 = invalid
  int m1_steps; // Commanded steps for M1. 0 = infinitive.
  int m2_steps; // Commanded steps for M2. 0 = infinitive.
  int m3_steps; // Commanded steps for M3. 0 = infinitive.
  struct t_command *prev; // Pointer to the previous (newer) command. If NULL this is the newest (head).
  struct t_command *next;  // Pointer to the next (older) command. If NULL this is the oldest (tail).
};

struct t_command_buffer {
  struct t_command *first; // The first command of the list (head).
  struct t_command *last;   // The last command of the list (tail).  
};

// The command buffer
struct t_command_buffer *command_buffer; // Pointer to the Command Buffer.

// The current inputs E1-E8
boolean e[8] = {LOW, LOW, LOW, LOW, LOW, LOW, LOW, LOW};
boolean d[3] = {LOW, LOW, LOW};

// The current motor tacho periods for motors M1-M3
unsigned long ts[3] = {0, 0, 0};

// CLK hold timer
unsigned long clkHold;

// Enable E1 as emergency OFF
boolean e1_emerg_off;

void setup()
{
  // Setup pins
  DDRB = _BV(LOAD_OUT); // register B out
  DDRD = _BV(CLK) | _BV(DATA_OUT) | _BV(LOAD_IN) | _BV(TRIGGER_X) | _BV(TRIGGER_Y); // register D out
  DDRB &= ~_BV(EMERG_OFF); // register B in
  DDRD &= ~_BV(DATA_IN); // register D in
  PORTB = _BV(EMERG_OFF); // register B pullups
  pinMode(ERROR_PIN, OUTPUT);

  // allocate Command Buffer
  digitalWrite(ERROR_PIN, LOW);
  if ((command_buffer = (struct t_command_buffer*)malloc(sizeof(struct t_command_buffer))) != NULL)
  {
    command_buffer->first = NULL;
    command_buffer->last = NULL;
  }
  else
  {
    digitalWrite(ERROR_PIN, HIGH);
  }

  // Enable/disable E1 for emergency off
  e1_emerg_off = PINB & _BV(EMERG_OFF);

  // Init Trigger X,Y
  PORTD |= _BV(TRIGGER_X);
  PORTD |= _BV(TRIGGER_Y);

  // init interface signals
  digitalInOut(0);

  // start CLK hold time
  clkHold = millis();

  // init Serial
  Serial.begin(19200);
  while (!Serial);
}

void loop_2()
{
  byte dauer = readAnalogY();
  Serial.println(dauer, DEC);
  delay(100);
}


void loop()
{
  // process next command from buffer
  struct t_command *command = removeCommandLast();
  if (command == NULL)
  {
    if (millis() - clkHold < 125)
    {
      writeDigitalInOutWithDelay(LOW, LOW, LOW, LOW);  
      writeDigitalInOutWithDelay(HIGH, LOW, LOW, LOW);  
    }
    return;
  }
  clkHold = millis();

  // read command data
  byte seqnr = command->seqnr;
  byte mcb = command->mcb;
  int m1_steps = command->m1_steps;
  int m2_steps = command->m2_steps;
  int m3_steps = command->m3_steps;
  free(command);

  // execute command
  executeCommand(seqnr, mcb, m1_steps, m2_steps, m3_steps);
}

void executeCommand(byte seqnr, byte mcb, int m1_steps, int m2_steps, int m3_steps)
{
  boolean m1_check = (mcb & M1) != 0 && m1_steps > 0;
  boolean m2_check = (mcb & M2) != 0 && m2_steps > 0;
  boolean m3_check = (mcb & M3) != 0 && m3_steps > 0;

  int m1_stop = 0;
  int m2_stop = 0;
  int m3_stop = 0;
   
  while(m1_check | m2_check | m3_check)
  {    
    // Command motors and read E1-E8.
    digitalInOut(mcb);
    
    // Emergency OFF
    if (e1_emerg_off && e[0] == HIGH)
    {
      mcb = 0;
      break;
    }
    
    // M1
    if (m1_check && d[0] == HIGH && e[1] == LOW) // e[1] = E2 = M1.
    {
      m1_stop = checkRPM(0, millis());
      m1_steps--;
      if (m1_steps <= m1_stop)
      {
        mcb = mcb & M1_OFF;
        m1_check = false;
      }
    }
    d[0] = e[1];

    // M2
    if (m2_check && d[1] == HIGH && e[3] == LOW) // e[3] = E4 = M2.
    {
      m2_stop = checkRPM(1, millis());
      m2_steps--;
      if (m2_steps <= m2_stop)
      {
        mcb = mcb & M2_OFF;
        m2_check = false;
      }
    }
    d[1] = e[3];
    
    // M3
    if (m3_check && d[2] == HIGH && e[5] == LOW) // e[5] = E6 = M3.
    {
      m3_stop = checkRPM(2, millis());
      m3_steps--;
      if (m3_steps <= m3_stop)
      {
        mcb = mcb & M3_OFF;
        m3_check = false;
      }
    }
    d[2] = e[5];
  }

  // run (remaining) motor command
  digitalInOut(mcb);

  // return digital and analog input values
  int ex = convToManchester(readAnalogX());
  int ey = convToManchester(readAnalogY());
  int di = convToManchester(convInputToByte());
  Serial.write(seqnr);
  Serial.write((byte) di & 0xFF); // LSB
  Serial.write((byte) (di >> 8) & 0xFF); // MSB
  Serial.write((byte) ex & 0xFF); // LSB
  Serial.write((byte) (ex >> 8) & 0xFF); // MSB
  Serial.write((byte) ey & 0xFF); // LSB
  Serial.write((byte) (ey >> 8) & 0xFF); // MSB
}

/**
 * Check the RPM.
 */
int checkRPM(int i, unsigned long timestamp)
{
  unsigned long ret = timestamp - ts[i];
  ts[i] = timestamp;
  if (ret < 16)
    return 2;
  else
    return 0;   
}

/**
 * return Inputs as a byte in the following form:
 * E1|E2|E3|E4|E5|E6|E7|E8, high-active (+5V = 1)
 */
byte convInputToByte()
{
  byte ebyte = 0;
  for (int i=0; i<8; i++)
  {
    ebyte = ebyte << 1;
    if (e[i] == LOW)
    {
      ebyte |= 1;
    }
  }
  return ebyte;
}

/**
 * Convert given byte into Manchester Code.
 */
int convToManchester(byte b)
{
  int ret = 0;
  byte mask = B10000000;
  for (int i=0; i<8; i++)
  {
    ret = ret << 2;
    if ((b & mask) == 0)
    {
      ret |= 1;
    }
    else
    {
      ret |= 2;
    }
    mask = mask >> 1;
  }
  return ret;
}

/**
 * Expects a command in the following form:
 *    $mcb,a,b,c\n
 *    
 *    $ = Start char.
 *    mcb = Motor Command Byte in decimal notation for M1-M4 (3 chars). 
 *    a = Commanded steps M1. (-32768..32767).
 *    b = Commanded steps M2. (-32768..32767).
 *    c = Commanded steps M3. (-32768..32767).
 *    d = Sequence Number (0-63).
 *    \n = Terminal char.
 *    
 *    Commanded steps = 0: Non-stepping run of corresponding motor.
 *    Commanded steps < 0: Motor off (overrides Motor Command Byte).
 */
void serialEvent()
{
  while (Serial.available() > 0)
  {
    // Check if memory for command is available
    struct t_command *command;
    if ((command = (struct t_command*)malloc(sizeof(struct t_command))) == NULL)
    {
      return;
    }
    command->prev = NULL;
    command->next = NULL;

    // read command
    if (Serial.read() == '$')
    {
      // next int is the motor command
      byte mcb = lowByte(Serial.parseInt());

      // Step count for M1
      int m1_steps = Serial.parseInt();
      if (m1_steps < 0)
      {
        mcb = mcb & M1_OFF;
      }
      command->m1_steps = m1_steps;
      
      // Step count for M2
      int m2_steps = Serial.parseInt();
      if (m2_steps < 0)
      {
        mcb = mcb & M2_OFF;
      }
      command->m2_steps = m2_steps;

      // Step count for M3
      int m3_steps = Serial.parseInt();
      if (m3_steps < 0)
      {
        mcb = mcb & M3_OFF;
      }
      command->m3_steps = m3_steps;
      command->mcb = mcb;

      // expect Terminal char or Sequence Number
      if (Serial.peek() == '\n')
      {
        Serial.read(); // Remove \n from buffer
        command->seqnr = 0;
        addCommandFirst(command);
      }
      else
      {
        // Sequence Number
        byte seqnr = lowByte(Serial.parseInt()) & B00111111;

        // Terminal char
        if (Serial.read() == '\n')
        {
          command->seqnr = seqnr;
          addCommandFirst(command);
        }      
      }
    } // Serial.read() == '$'
    
    // if command was not added, free memory
    if (command_buffer == NULL || command_buffer->first != command)
    {
      free(command);
    }
  } // while
}

/**
 * Adds a new command before the head of the list.
 */
void addCommandFirst(struct t_command *newCommand)
{
  if (command_buffer == NULL || newCommand == NULL)
  {
    return;
  }

  if (command_buffer->last == NULL)
  {
    command_buffer->last = newCommand;
  }
  else
  {
    struct t_command *head = command_buffer->first;
    newCommand->next = head;    
    head->prev = newCommand;
  }
  command_buffer->first = newCommand;
}

/**
 * Removes the last (tail) command from the list.
 * To free the memory of the returned command is in the responsibility of the caller.
 */
struct t_command* removeCommandLast()
{
  if (command_buffer == NULL)
  {
    return NULL;
  }

  struct t_command *last = command_buffer->last;  
  if (last != NULL)
  {
    if (last->prev != NULL)
    {
      last->prev->next = NULL;
    }
    else
    {
      command_buffer->first = NULL;
    }
    command_buffer->last = last->prev;
  }
  return last;
}

/**
 * read and write digital in and out.
 */
void digitalInOut(byte mcb)
{
  // unsigned long start = micros();
  byte mcb_mask = B00000001; // M4 first, Bit 0
  boolean data_out;
  boolean load_in;

  // read and write digital input and output
  for (int i=7; i>=0; i--)
  {
    data_out = (mcb & mcb_mask) != 0;
    load_in = (i == 7);
    writeDigitalInOutWithDelay(LOW, LOW, data_out, load_in);
    writeDigitalInOutWithDelay(HIGH, LOW, data_out, load_in);
    e[i] = PIND & _BV(DATA_IN); // E8 = e[7] first
    mcb_mask = mcb_mask << 1;
  }
  // Strobe data from shift register to storage register
  writeDigitalInOutWithDelay(LOW, HIGH, LOW, LOW);
  writeDigitalInOutWithDelay(HIGH, LOW, LOW, LOW);
}

/**
 * writes the signals for digital in and out.
 */
void writeDigitalInOutWithDelay(boolean clk, boolean load_out, boolean data_out, boolean load_in)
{
  if (data_out)
    PORTD |= _BV(DATA_OUT);
  else
    PORTD &= ~_BV(DATA_OUT);

  if (load_out)
    PORTB |= _BV(LOAD_OUT);
  else
    PORTB &= ~_BV(LOAD_OUT);
  
  if (load_in)
    PORTD |= _BV(LOAD_IN);
  else
    PORTD &= ~_BV(LOAD_IN);
  
  if (clk)
  {
    PORTD |= _BV(CLK);
    delayMicroseconds(28); // 24
  }
  else
  {
    PORTD &= ~_BV(CLK);
    delayMicroseconds(20); // 24
  }
}

/**
 * Read EX
 */
byte readAnalogX()
{
  boolean d = true;
  boolean count_in;

  // CLK to reset Q8
  writeAnalogInWithDelay(LOW, HIGH, HIGH);
  
  // trigger timer
  writeAnalogInWithDelay(HIGH, LOW, HIGH);
  writeAnalogInWithDelay(HIGH, HIGH, HIGH);

  // count cycles while DATA_IN is LOW (Rmax ~10K)
  for(int i=-10; i<255; i++)
  {
    count_in = PIND & _BV(DATA_IN);
    if (d == false && count_in == true)
    {
      return i < 0 ? 0 : i;      
    }
    d = count_in;
    delayMicroseconds(20);
  }
  // wait until DATA_IN is HIGH
  while (!(PIND & _BV(DATA_IN)));
  return 255;
}

/**
 * Read EY
 */
byte readAnalogY()
{
  boolean d = true;
  boolean count_in;

  // CLK to reset Q8
  writeAnalogInWithDelay(LOW, HIGH, HIGH);
  
  // trigger timer
  writeAnalogInWithDelay(HIGH, HIGH, LOW);
  writeAnalogInWithDelay(HIGH, HIGH, HIGH);

  // count cycles while DATA_IN is LOW (Rmax ~10K)
  for(int i=-10; i<255; i++)
  {
    count_in = PIND & _BV(DATA_IN);
    if (d == false && count_in == true)
    {
      return i < 0 ? 0 : i;      
    }
    d = count_in;
    delayMicroseconds(20);
  }
  // wait until DATA_IN is HIGH
  while (!(PIND & _BV(DATA_IN)));
  return 255;
}

/**
 * Writes the signals to read analog inputs.
 */
void writeAnalogInWithDelay(boolean clk, boolean trigger_x, boolean trigger_y)
{
  if (clk)
    PORTD |= _BV(CLK);
  else
    PORTD &= ~_BV(CLK);
  
  if (trigger_x)
    PORTD |= _BV(TRIGGER_X);
  else
    PORTD &= ~_BV(TRIGGER_X);

  if (trigger_y)
    PORTD |= _BV(TRIGGER_Y);
  else
    PORTD &= ~_BV(TRIGGER_Y);
  delayMicroseconds(8);
}





