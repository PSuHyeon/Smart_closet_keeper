from picamera import picamera
from time import sleep

camera = PiCamera()
sleep(1)
camera.capture('photo.jpg')