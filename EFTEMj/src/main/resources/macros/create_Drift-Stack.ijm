newImage("Drift-Stack_max64px", "32-bit black", 256, 256, 10);
width = 32;
height = 32;
x = 112;
y = 112;
x_start = 128;
y_start = 128;
makeRectangle(x,y,width,height);
run("Set...", "value=255 slice");
run("Set Label...", "label=[" + x + "," + y + "]");
for (i = 2; i<=10;i++) {
	setSlice(i);
	x = x_start + round(random * 64) - 32 - width/2;
	y = y_start + round(random * 64) - 32 - height/2;
	// print(x, y);
	makeRectangle(x,y,width,height);
	run("Set...", "value=255 slice");
	run("Set Label...", "label=[" + x + "," + y + "]");
}
makeRectangle(64,64,128,128);
run("Start Animation [\\]");