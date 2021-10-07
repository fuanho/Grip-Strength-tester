import machine
import utime
from machine import ADC, UART, Pin, I2C, Timer

from ssd1306 import SSD1306_I2C

pressure_x = ADC(27)
pressure_y = ADC(26)
BT = UART(0, 9600)
internal_led = Pin(25, Pin.OUT)
counter_A = Pin(2, Pin.OUT)
counter_B = Pin(3, Pin.OUT)
sda = Pin(4)
scl = Pin(5)
i2c = I2C(0, scl=scl, sda=sda, freq=400000)
oled = SSD1306_I2C(128, 64, i2c)
print(machine.UART(0))
tim = Timer()


message: str = ""
count: int = 0
conversion_factor = 3.3 / 65535


def get_pressure(a: int, b: int):
    counter_A.value(a)
    counter_B.value(b)
    read_x = pressure_x.read_u16()
    read_y = pressure_y.read_u16()
    return read_x, read_y


def send_to_bt(msg: str):
    try:
        BT.write(bytearray(msg))
        print('Message scened!')
    except:
        print('Some thing wrong!!!')


def get_from_bt():
    if BT.any():
        return BT.read()
    else:
        return None


def tick(timer):
    global message
    global count
    message = ""
    count += 1
    oled.fill(0)
    oled.text("G.S. Testing", 0, 0)
    for i in range(3):
        if i == 0:
            read_x, read_y = get_pressure(0, 0)
            message = "{}\n".format(3.3 - conversion_factor)
            print(message)
            send_to_bt(message=message)
        elif i == 1:
            read_x, read_y = get_pressure(0, 1)
        else:
            read_x, read_y = get_pressure(1, 0)
        tmp_x = 3.3 - read_x * conversion_factor
        tmp_y = 3.3 - read_y * conversion_factor
        tmp = '{index}. X {x:.1f} Y {y:.1f}'.format(index=i + 1, x=tmp_x, y=tmp_y)

        oled.text(tmp, 0, (i * 10)+35)

    oled.show()


tim.init(freq=50, mode=Timer.PERIODIC, callback=tick)
