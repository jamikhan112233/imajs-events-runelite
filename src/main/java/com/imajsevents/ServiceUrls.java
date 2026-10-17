package com.imajsevents;

final class ServiceUrls
{
	static final String SERVICE_BASE_URL = "https://imajs-events-service.imajs-events.workers.dev";

	private ServiceUrls()
	{
	}

	static String endpoint(String path)
	{
		if (path == null || !path.startsWith("/"))
		{
			throw new IllegalArgumentException("Service path must start with /");
		}
		return SERVICE_BASE_URL + path;
	}
}
