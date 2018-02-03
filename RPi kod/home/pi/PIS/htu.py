#!/usr/bin/python

import array
import time
import i2c_base
from time import sleep
import RPi.GPIO as GPIO
from decimal import Decimal
import paho.mqtt.publish as publish

GPIO.setmode(GPIO.BCM) #Set GPIO mode to BCM
GPIO.setwarnings(False)

## HTU21D begin ##

HTU21D_ADDR = 0x40
CMD_READ_TEMP_HOLD = b"\xE3"
CMD_READ_HUM_HOLD = b"\xE5"
CMD_READ_TEMP_NOHOLD = b"\xF3"
CMD_READ_HUM_NOHOLD = b"\xF5"
CMD_WRITE_USER_REG = b"\xE6"
CMD_READ_USER_REG = b"\xE7"
CMD_SOFT_RESET = b"\xFE"

class HTU21D(object):
    def __init__(self):
        self.dev = i2c_base.i2c(HTU21D_ADDR, 1)  # HTU21D 0x40, bus 1
        self.dev.write(CMD_SOFT_RESET)  # Soft reset
        time.sleep(.1)

    def ctemp(self, sensor_temp):
        t_sensor_temp = sensor_temp / 65536.0
        return -46.85 + (175.72 * t_sensor_temp)
      
    def chumid(self, sensor_humid):
        t_sensor_humid = sensor_humid / 65536.0
        return -6.0 + (125.0 * t_sensor_humid)
      
    def temp_coefficient(self, rh_actual, temp_actual, coefficient=-0.15):
        return rh_actual + (25 - temp_actual) * coefficient

    def crc8check(self, value):
        # Ported from Sparkfun Arduino HTU21D Library:
        # https://github.com/sparkfun/HTU21D_Breakout
        remainder = ((value[0] << 8) + value[1]) << 8
        remainder |= value[2]

        # POLYNOMIAL = 0x0131 = x^8 + x^5 + x^4 + 1 divisor =
        # 0x988000 is the 0x0131 polynomial shifted to farthest
        # left of three bytes
        divisor = 0x988000

        for i in range(0, 16):
            if(remainder & 1 << (23 - i)):
                remainder ^= divisor
            divisor = divisor >> 1

        if remainder == 0:
            return True
        else:
            return False

    def read_temperature(self):
        self.dev.write(CMD_READ_TEMP_NOHOLD)  # Measure temp
        time.sleep(.1)
        data = self.dev.read(3)
        buf = array.array('B', data)
        if self.crc8check(buf):
            temp = (buf[0] << 8 | buf[1]) & 0xFFFC
            return self.ctemp(temp)
        else:
            return -255

    def read_humidity(self):
        temp_actual = self.read_temperature()  # For temperature coefficient compensation
        self.dev.write(CMD_READ_HUM_NOHOLD)  # Measure humidity
        time.sleep(.1)
        data = self.dev.read(3)
        buf = array.array('B', data)

        if self.crc8check(buf):
            humid = (buf[0] << 8 | buf[1]) & 0xFFFC
            rh_actual = self.chumid(humid)

            rh_final = self.temp_coefficient(rh_actual, temp_actual)
            rh_final -= 80
            
            rh_final = 100.0 if rh_final > 100 else rh_final  # Clamp > 100
            rh_final = 0.0 if rh_final < 0 else rh_final  # Clamp < 0

            return rh_final
        else:
            return -255
          
## HTU21D end ##


##  ThingSpeak Channel Settings begin ##

channelID = "414644"

# The Write API Key for the channel
apiKey = "1CQCOY7S4AH44WQN"

#  MQTT Connection Methods

# Set useUnsecuredTCP to True to use the default MQTT port of 1883
# This type of unsecured MQTT connection uses the least amount of system resources.
useUnsecuredTCP = False

# Set useUnsecuredWebSockets to True to use MQTT over an unsecured websocket on port 80.
# Try this if port 1883 is blocked on your network.
useUnsecuredWebsockets = False

# Set useSSLWebsockets to True to use MQTT over a secure websocket on port 443.
# This type of connection will use slightly more system resources, but the connection
# will be secured by SSL.
useSSLWebsockets = True


# The Hostname of the ThingSpeak MQTT service
mqttHost = "mqtt.thingspeak.com"

# Set up the connection parameters based on the connection type
if useUnsecuredTCP:
    tTransport = "tcp"
    tPort = 1883
    tTLS = None

if useUnsecuredWebsockets:
    tTransport = "websockets"
    tPort = 80
    tTLS = None

if useSSLWebsockets:
    import ssl
    tTransport = "websockets"
    tTLS = {'ca_certs':"/etc/ssl/certs/ca-certificates.crt",'tls_version':ssl.PROTOCOL_TLSv1}
    tPort = 443
        
# Create the topic string
topic = "channels/" + channelID + "/publish/" + apiKey

## ThingSpeak Channel Settings end ##

def round_perc(number):
           x = Decimal(number)
           x = round(x,2)
           return x

#HTU21D init
obj = HTU21D()
    
while(True):
    try:
        temp = obj.read_temperature()   #read temperature and round to 2 number after decimal point
        temperature = round_perc(temp)

        
        humid = obj.read_humidity()     # read humidity and round to 2 number after decimal point
        humidity = round_perc(humid)

        print("Temp: ", temperature)
        print("Hum: ", humidity)

        # build the payload string
        tPayload = "field1=" + str(temperature) + "&field2=" + str(humidity)

        # attempt to publish this data to the topic 
        publish.single(topic, payload=tPayload, hostname=mqttHost, port=tPort, tls=tTLS, transport=tTransport)
        sleep(25)

    except (KeyboardInterrupt):
        print("\nEND")
        sleep(0.1)
        GPIO.cleanup()
        break

    except:
        print ("ERROR")
        sleep(5)
