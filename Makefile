all: build

run: build
	java compare

build:
	javac compare.java

clean:
	rm -rf *.class fast.dat smooth.dat