import socket
from pyowm import OWM
import requests
import json

server_address = "10.0.1.12"
port = 8040
size = 1024

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind((server_address, port))
sock.listen(1)

print("Waiting ev3Client...")

try:
    client, clientInfo = sock.accept()
    print("Connected client",clientInfo)
   
    data = client.recv(size).decode("UTF-8")
    if data:
        print(data)
        api = "https://api.openweathermap.org/data/2.5/weather?q=Daejeon&appid=dbbdace3c053ea4457e877481a9f9af1"
        result = requests.get(api)
        result = json.loads(result.text)
        yay = result['weather'][0]['main']
        temp = yay.encode("UTF-8")
        client.send(len(temp).to_bytes(2, byteorder='big'))
        client.send(temp)
        print("sent")
        print(temp)

    else:
        print("Disconnected")
        client.close()
        sock.close()

except:
    print("Closing socket")
    client.close()
    sock.close()