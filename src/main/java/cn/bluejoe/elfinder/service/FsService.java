package cn.bluejoe.elfinder.service;

import java.io.IOException;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;

public interface FsService
{
	FsItem fromHash(String hash) throws IOException;

	String getHash(FsItem item) throws IOException;

	FsSecurityChecker getSecurityChecker();

	String getVolumeId(FsVolume volume);

	FsVolume[] getVolumes();

	FsServiceConfig getServiceConfig();

    FsItemEx[] find(FsItemFilter filter, FsItemEx target, boolean recursive);
}