Be sure to change the network interface from "en0" (en0 is common for MacOS) to what network inerface you have on your device.
You find where to change network interface on line 16 inside MulticastCollector.java

To check which network interface you have on your device, you can run the following command in the terminal:

MacOS:
```bash
networksetup -listallhardwareports
```

Windows (Powershell):
```bash
Get-NetAdapter
```

External libs used:

Flatlaf:

https://github.com/JFormDesigner/FlatLaf?tab=readme-ov-file
