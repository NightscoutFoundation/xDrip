package com.eveningoutpost.dexdrip.utils;

import android.os.Environment;

import java.io.File;

public class FileUtils {

	public static boolean makeSureDirectoryExists( final String dir ) {
		final File file = new File( dir );
        return file.exists() || file.mkdirs();
	}

	public static String getExternalDir() {
		final StringBuilder sb = new StringBuilder();
		sb.append( Environment.getExternalStorageDirectory().getAbsolutePath() );
		sb.append( "/xdrip" );

		final String dir = sb.toString();
		return dir;
	}

	public static String combine( final String path1, final String path2 ) {
		final File file1 = new File( path1 );
		final File file2 = new File( file1, path2 );
		return file2.getPath();
	}
}
