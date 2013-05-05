img-dup
=======

Detect duplicate images in a folder. This is done by resizing all images to 256x256 grayscale and finding the average pixel difference. It displays pairings of images sorted by the average pixel differences.

Usage
-----
To run this code, copy everything into the folder with the images and run 'make run'. Alternatively, you can just copy the *.class files and run with 'java compare'.

To run with the slow flag, use 'java compare . slow'. This uses Image.SCALE_SMOOTH instead of Image.SCALE_FAST to scale the images. This may give more accurate results.

Command Line Arguments
----------------------
java compare [path_to_images] [slow]