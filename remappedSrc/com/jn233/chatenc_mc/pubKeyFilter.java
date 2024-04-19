package com.jn233.chatenc_mc;

import java.io.File;
import java.io.FileFilter;

public class pubKeyFilter implements FileFilter {
	private final String pkRegex = ".*\\-.*\\.pk";
	private final String spkRegex = ".*\\-.*\\.spk";
	@Override
	public boolean accept(File pathname) {
		if ((pathname.getName().toLowerCase().matches(pkRegex) || pathname.getName().toLowerCase().matches(spkRegex) )
				&& pathname.isFile() 
				&& pathname.canRead()
				) {
			return true;
		}
		return false;
	}

}
