import os
import glob
from time import sleep
import RPi.GPIO as GPIO
from bluetooth import *
import array
import i2c_base
from decimal import Decimal

GPIO.setmode(GPIO.BCM)
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
        sleep(.1)

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
        sleep(.1)
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
        sleep(.1)
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

#HTU21D init
obj = HTU21D()

## Bluetooth ##
connection = False
server_sock=BluetoothSocket( RFCOMM )
server_sock.bind(("",PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "eb12b754-00e8-11e8-ba89-0ed5f89f718b"

advertise_service( server_sock, "BluetoothRPiServer",
                   service_id = uuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ] 
#                   protocols = [ OBEX_UUID ] 
                    )

## Bluetooth ##

def roundToTwo(number):
           x = Decimal(number)
           x = round(x,2)
           return x

        
while True:           
	if(connection == False):
		print("Waiting for connection on RFCOMM channel %d" % port)
		client_sock, client_info = server_sock.accept()
		connection = True
		print("Accepted connection from ", client_info)
	try:
        	data = client_sock.recv(1024)
       		if (data == "poweroff"):
			client_sock.close()
			connection = False
			sleep(1)
			os.system("sudo poweroff")

                if (data=="temp"):
                        temp = obj.read_temperature()
                        temperature = roundToTwo(temp)
                        print(temperature)
                        client_sock.send("%s" % temperature)
                        connection = False

                if(data=="hum"):
                        humid = obj.read_humidity()
                        humidity = roundToTwo(humid)
                        print(humidity)
                        client_sock.send("%s" % humidity)
                        connection = False

                else:
                       connection = False
                       pass

	except IOError:
    		print("Connection disconnected!")
		client_sock.close()
		connection = False
		pass
	    
	except BluetoothError:
		print("Something went wrong with bluetooth")
		client_sock.close()
		connection = False
		pass
		
	except KeyboardInterrupt:
		print("\nDisconnected")
		client_sock.close()
		server_sock.close()
		break
