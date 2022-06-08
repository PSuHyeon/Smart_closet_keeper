import socket
from pyowm import OWM
import requests
import json
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
import torchvision
import torchvision.transforms as transforms
import pandas as pd
from collections import OrderedDict
from IPython.display import clear_output
import numpy as np
import cv2 as cv
from PIL import Image
from torchvision.transforms import ToTensor, ToPILImage
from picamera import PiCamera
from time import sleep

learning_rate = 0.001
batch_size = 100
num_classes = 10
epochs = 5
#Section 4
class ConvNet(nn.Module):
    def __init__(self):
        super().__init__()
        self.layer1 = nn.Sequential( # 순차적인 레이어 쌓게 함
            # Convolution + ReLU + max Pool
            nn.Conv2d(in_channels=1, out_channels=32, kernel_size=5, stride=1, padding=2),
            # Wout = (Win - FilterSize + 2*Padding)/Stride + 1
            nn.ReLU(),
            nn.MaxPool2d(kernel_size=2, stride=2)
        )
        self.layer2 = nn.Sequential( # 순차적인 레이어 쌓게 함
            # Convolution + ReLU + max Pool
            nn.Conv2d(in_channels=32, out_channels=64, kernel_size=5, stride=1, padding=2),
            nn.ReLU(),
            nn.MaxPool2d(kernel_size=2, stride=2)
        )
        self.dropout = nn.Dropout() # over-fitting 방지 
        self.fc1 = nn.Linear(in_features=7*7*64, out_features=1000)
        self.fc2 = nn.Linear(in_features=1000, out_features=10)
        
    def forward(self, x):
        x = self.layer1(x)
        x = self.layer2(x)
        x = x.reshape(x.size(0), -1)
        x = self.dropout(x) # 오버피팅을 막기 위해 학습 과정시 일부 뉴런을 생략하는 기능 
        x = self.fc1(x)
        x = self.fc2(x)
        return x
'''
#***Important!***
#This part is for first train

#Section 5
model = ConvNet()
critertion = nn.CrossEntropyLoss()
optimizer = torch.optim.Adam(model.parameters(), lr = learning_rate)

#Section 6

#Section 7
#Model train
total_step = len(train_loader)
pd_results = []

for epoch in range(epochs):
    for i, (images, labels) in enumerate(train_loader):
        out = model(images)
        loss = critertion(out, labels)
        
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()
        
        total = labels.size(0)
        preds = torch.max(out.data, 1)[1] 
        correct = (preds==labels).sum().item() 
        
        if (i+1)%100==0:
            results = OrderedDict()
            results['epoch'] = epoch+1
            results['idx'] = i+1
            results['loss'] = loss.item()
            results['accuracy'] = 100.*correct/total
            pd_results.append(results)
            #df = pd.DataFrame.from_dict(pd_results, orient='columns')

            clear_output(wait=True)
            #display(df)

#Section 8
#Save the model
torch.save({'model': model.state_dict()}, 'model.pth')
'''

def getClothesType():
    camera = PiCamera()
    model = ConvNet()
    #Section 9
    #test the model
    model.eval() # evaluate mode로 전환 dropout 이나 batch_normalization 해제
    #Section 10
    #Pycamera
    #Take a picture and save it
    sleep(1)
    camera.capture('photo.jpg')
    #Section 11


    img = cv.imread("photo.jpg")

    # maintain the color...
    img = cv.cvtColor(img, cv.COLOR_BGR2GRAY)
    mid = img[240,360]
    boundary = img[10,10]
    #cv2_imshow(img)
    if mid>=boundary:
        img = cv.resize(img, (28,28))
        #img = img.flatten().reshape(1, 1, 28, 28)
        convert_tensor = transforms.ToTensor()
        img = convert_tensor(img)
        img = img.flatten().reshape(1, 1, 28, 28)
    else:
        img = cv.bitwise_not(img)
        #cv2_imshow(img)
        img = cv.resize(img, (28, 28))
        convert_tensor = transforms.ToTensor()
        img = convert_tensor(img)
        img = img.flatten().reshape(1, 1, 28, 28)



    model = ConvNet()
    state_dict = torch.load('model.pth')
    model.load_state_dict(state_dict['model'])
    #model save

    #real data input and output
    out = model(img) # 자동으로 forward 함수 불러옴
    realpreds = torch.argmax(out.data)
    predsvalue = torch.max(out.data)
    preds = torch.max(out.data, 1)[1]
    print('The image is: ', preds)
    typeIndex = int(preds.find('['))
    
    if typeIndex % 2 == 0 or typeIndex == 3:
        returnType = "Top"
    elif typeIndex == 1:
        returnType = "Bottom"
    else:
        returnType = "Others..."
        #sandal or sneaker or ankleBoot
    print(returnType)
    return returnType



server_address = "10.0.1.12"
port = 8040
size = 1024

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind((server_address, port))
sock.listen(1)

print("Waiting ev3Client...")
while True :
    try:
        client, clientInfo = sock.accept()
        print("Connected client",clientInfo)
        
        data = client.recv(size).decode("UTF-8")
        print(data)
        if ("0" in data):
            print("weather case")
            api = "https://api.openweathermap.org/data/2.5/weather?q=Daejeon&appid=dbbdace3c053ea4457e877481a9f9af1"
            result = requests.get(api)
            result = json.loads(result.text)
            yay = result['weather'][0]['main']
            temp = yay.encode("UTF-8")
            client.send(len(temp).to_bytes(2, byteorder='big'))
            client.send(temp)
            print("sent")
            print(temp)
        elif "1" in data:
            print("clothesType case")
            clothesType = getClothesType()
            temp = clothesType.encode("UTF-8")
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



