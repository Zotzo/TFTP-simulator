# TFTP
    This is a TFTP Client and server that is used for uploading and downloading files. 
    Below you will find instructions on how to run this program.

## Windows:
* 1: Download this repository
* 2: Set the port inside TFTPServer.java to your preferred port.
* 3: Install the latest version of [this](https://pjo2.github.io/tftpd64/) software.
* 4: Open the software, navigate to the TFTP client tab, input your localhost (127.0.0.1) as the host and set the port to the same port you applied in step 2.
* 5: Run TFTPServer.java.

### If you want to read:

* The local field tab in the software is the path to where you want to store the read file.
* The remote field is where you input the file you want to read in the server.
* After completing the previous steps, press GET and you will find your file in the same path you input previously.

### If you want to write:

* The local field tab is the file path you want to upload to the server. 
* The remote field is the name of the file that is to be uploaded to the server.
* After completing the previous tasks, press PUT and you will find your file within the write folder.


## Mac / Linux

TFTP generally comes pre-installed in linux or MacOS. Thus there is no need to download any program from an extenal party. You will use the command line interface for this program. 

* 1: Download this repository
* 2: Set the port inside TFTPServer.java to your preferred port.
* 3: Run TFTPServer.java.
* 4: Open your command line prompt and type in tftp.
* 5: Write "connect localost (and enter the same ip as previously applied)".
* 6: Write "mode octet" to specify the mode of file transfer.

### If you want to read

* You write "get (FILENAME)"

### If you want to write

* You write "put (FILENAME)"