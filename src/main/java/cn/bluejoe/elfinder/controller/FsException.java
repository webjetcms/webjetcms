package cn.bluejoe.elfinder.controller;

import java.io.IOException;

public class FsException extends IOException
{
	private static final long serialVersionUID = 5394756753686633760L;

	public FsException(String message)
	{
		super(message);
	}

	public FsException(String message, Throwable e)
	{
		super(message, e);
	}

}
