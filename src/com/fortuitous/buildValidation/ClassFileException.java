/* Copyright 2018 Harold Fortuin of
   Fortuitous Consulting Services, Inc.

   You are free to use or modify this software and source code
   as long as you include this Copyright notice.

   No warranty is provided or implied. Use at your own risk.
*/
package com.fortuitous.buildValidation;

class ClassFileException extends Exception {

	// to please Eclipse
	static final long serialVersionUID = 927394629374924L;
	
	ClassFileException(String message) {
        super(message);
    }
}
