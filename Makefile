all: build

run: build
	java compare

nobuild:
	java compare

slow:
	java compare . slow

build:
	javac compare.java

clean:
	rm -rf *.class fast.dat smooth.dat
