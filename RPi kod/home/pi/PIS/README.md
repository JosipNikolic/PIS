Paython skripte koje se izvršavaju na Raspberry-u.

Sktruktura direktorija kao na linux distribucijama.
Raspberry pokreće Raspbian operacijski sustav.

Izmjene u Raspbian sustavu:
	1. Pokretanje u CLI modu (raspi-config -> boot options)
	2. Za pokretanje skripti nakon bootanja u datoteku /etc/profile dodano:
		sudo python /home/pi/PIS/pis.py & sudo python /home/pi/PIS/htu.py