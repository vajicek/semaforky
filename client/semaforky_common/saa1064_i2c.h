/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include <Arduino.h>

#ifdef INVERTED_WIRE_LOGIC
  #define i2c_sda_lo() digitalWrite(sda_pin_, HIGH);
  #define i2c_scl_lo() digitalWrite(scl_pin_, HIGH);
  #define i2c_sda_hi() digitalWrite(sda_pin_, LOW);
  #define i2c_scl_hi() digitalWrite(scl_pin_, LOW);
#else
  #define i2c_sda_lo() digitalWrite(sda_pin_, LOW);
  #define i2c_scl_lo() digitalWrite(scl_pin_, LOW);
  #define i2c_sda_hi() digitalWrite(sda_pin_, HIGH);
  #define i2c_scl_hi() digitalWrite(scl_pin_, HIGH);
#endif

class I2C_BitBanging {
public:

  void i2c_writebit(uint8_t c) {
    i2c_scl_lo();
    delayMicroseconds(5);
    if (c == 0) {
      i2c_sda_lo();
    } else {
      i2c_sda_hi();
    }
    delayMicroseconds(i2cdelay_);
    i2c_scl_hi();
    delayMicroseconds(i2cdelay_);
  }

  void i2c_write_ack() {
    i2c_writebit(0);
  }

  void i2c_write(uint8_t c) {
    for (uint8_t i = 0; i < 8; i++) {
      i2c_writebit(c & 0x80); // 2 * delay
      c <<= 1;
    }
    i2c_write_ack(); //2 * delay

    // epilog
    delayMicroseconds(i2cdelay_ * 2);
  }

  void i2c_stop() {
    i2c_scl_lo();
    i2c_sda_lo();
    delayMicroseconds(i2cdelay_ * 2);
    i2c_scl_hi();
    delayMicroseconds(i2cdelay_);
    i2c_sda_hi();
    delayMicroseconds(i2cdelay_ * 2);
  }

  void i2c_start() {
    i2c_sda_hi();
    i2c_scl_hi();
    delayMicroseconds(i2cdelay_ * 2);
    i2c_sda_lo();
    delayMicroseconds(i2cdelay_);
    i2c_scl_lo();
    delayMicroseconds(i2cdelay_ * 2);
  }

  void i2c_address(uint8_t c) {
    i2c_write(c);
  }

  int scl_pin_, sda_pin_;
  int i2cdelay_;

  I2C_BitBanging(int delay, int scl_pin, int sda_pin)
    :i2cdelay_(delay), scl_pin_(scl_pin), sda_pin_(sda_pin) {
  }
};

/// 0-9 digit to data byte mapping.
byte digit_to_data[] = {
  64,  //-
  0,   //blank
  63,  //0
  6,
  91,
  79,
  102,
  109,
  125,
  7,
  127,
  111, //9
};

/// Write our digits to I2C bus (without waiting for ACK)
class SAA1064 : public I2C_BitBanging {
  bool initialized;
public:
  SAA1064(int delay, int scl_pin, int sda_pin) :
    I2C_BitBanging(delay, scl_pin, sda_pin), initialized(false) {
  }
  void init() {
    pinMode(scl_pin_, OUTPUT);
    pinMode(sda_pin_, OUTPUT);

    i2c_start();
    i2c_address(0x70);
    i2c_write(0); // instruction byte + autonicrement
    i2c_write(0x30 | 7); // control byte
    i2c_stop();
  }
  void digits(int p1, int p2, int p3, int p4) {
    if (!initialized) {
      init();
      initialized = true;
    }
    i2c_start();
    i2c_address(0x70);
    i2c_write(1); // instruction byte + autonicrement
    i2c_write(digit_to_data[p1 + 2]);
    i2c_write(digit_to_data[p2 + 2]);
    i2c_write(digit_to_data[p3 + 2]);
    i2c_write(digit_to_data[p4 + 2]);
    i2c_stop();
  }
};
