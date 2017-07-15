/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include <Arduino.h>

#define i2c_sda_lo() digitalWrite(sda_pin_, LOW);
#define i2c_scl_lo() digitalWrite(scl_pin_, LOW);
#define i2c_sda_hi() digitalWrite(sda_pin_, HIGH);
#define i2c_scl_hi() digitalWrite(scl_pin_, HIGH);

class I2C_BitBanging {
public:

  void clock() {
    for(int i = 0; i < 100; i++) {
      i2c_scl_lo();
      delayMicroseconds(i2cdelay_);
      i2c_scl_hi();
      delayMicroseconds(i2cdelay_);
    }
  }

  void i2c_writebit(uint8_t c) {
    i2c_scl_lo();
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
    i2c_scl_lo();
    i2c_sda_hi();
    delayMicroseconds(i2cdelay_/2);
    i2c_sda_lo();
    delayMicroseconds(i2cdelay_/2);
    i2c_scl_hi();
    delayMicroseconds(i2cdelay_);
    i2c_scl_lo();
  }

  void i2c_write(uint8_t c) {
    for (uint8_t i = 0; i < 8; i++) {
      i2c_writebit(c & 0x80);
      c <<= 1;
    }
    i2c_write_ack();

    // epilog
    delayMicroseconds(i2cdelay_ * 2);
  }

  void i2c_address(uint8_t c) {
    // prolog
    i2c_scl_hi();
    i2c_sda_hi();
    delayMicroseconds(i2cdelay_ * 2);
    i2c_sda_lo();
    delayMicroseconds(i2cdelay_);
    i2c_scl_lo();
    delayMicroseconds(i2cdelay_ * 2);

    i2c_write(c);

    // epilog
    delayMicroseconds(i2cdelay_ * 2);
  }

  int scl_pin_, sda_pin_;
  int i2cdelay_;

  I2C_BitBanging(int delay, int scl_pin, int sda_pin)
    :i2cdelay_(delay), scl_pin_(scl_pin), sda_pin_(sda_pin)
    {
      pinMode(scl_pin_, OUTPUT);
      pinMode(sda_pin_, OUTPUT);
  }
};

byte digit_to_data[] = {
  0,
  6,
  91,
  79,
  102,
  109,
  125,
  7,
  127,
  111
};

class SAA1064 : public I2C_BitBanging {
public:
  SAA1064(int delay, int scl_pin, int sda_pin) :
    I2C_BitBanging(delay, scl_pin, sda_pin) {
  }
  void digits(byte p1, byte p2, byte p3, byte p4) {
    i2c_address(112);
    i2c_write(1); // instruction byte
    i2c_write(digit_to_data[p4]);
    i2c_write(digit_to_data[p3]);
    i2c_write(digit_to_data[p2]);
    i2c_write(digit_to_data[p1]);
  }
};
