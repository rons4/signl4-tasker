# Tasker Examples

This is just a repo where I share some [Tasker](https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm) files. It is mainly indended to show integration capabilities in conjunction with [SIGNL4](https://www.signl4.com/) for reliable mobile alerting.

Discplaimer: No guarantee if this works. Please use at own risk.

## Floating Button

Some time ago I searched for a way to get floating buttons work with Tasker. There are some ways but I did not really found a nice approach.
Now, I (together with ChatGPT) was able to create a nice floating button (bubble) using Tasker's Java code support.

This is how it looks like: https://youtube.com/shorts/1DSYow3Y1xM

And here is the Java code for showing the button:
https://raw.githubusercontent.com/rons4/signl4-tasker/refs/heads/main/tasker-show-floating-button.java

And here for closing the button again:
https://raw.githubusercontent.com/rons4/signl4-tasker/refs/heads/main/tasker-close-floating-button.java

## Usage

Put the first Java action into one task. This one will show the button. The second Java code can go into a second action to close the button.

The Java code sends two intents:
- On tab: com.example.MY_BUBBLE
- On long tab: com.example.MY_BUBBLE_LONG

You can use Tasker profiles to do whatever you want. In my case, I send a SIGNL4 alert it the button is pressed and I close the button if the button is long pressed.

Attention: This is just a quick example with no guarantee that it works as expected. Also, you might want to adapt the code to add other or additional functionality.

## MQTT Messages

This is a simple Jave code to send MQTT messages to an MQTT broker from Tasker:  
https://raw.githubusercontent.com/rons4/signl4-tasker/refs/heads/main/tasker-send-mqtt.java

It can be used for example to integrate your android phone via Tasker with [Node-RED](https://nodered.org/).

Note the configuration section to adapt IP, port and MQTT topic. the Tasker variable %mptt_value vontains the value to send.

