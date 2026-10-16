package com.imajsevents;

final class ServiceUrls
{
	private ServiceUrls()
	{
	}

	static String normalizeBaseUrl(String raw)
	{
		String value = raw == null ? "" : raw.trim();
		while (value.endsWith("/"))
		{
			value = value.substring(0, value.length() - 1);
		}
		if (!value.startsWith("https://") && !value.startsWith("http://localhost") && !value.startsWith("http://127.0.0.1"))
		{
			throw new IllegalArgumentException("Use an HTTPS service URL");
		}
		return value;
	}

	static String endpoint(String rawBaseUrl, String path)
	{
		return normalizeBaseUrl(rawBaseUrl) + path;
	}
}
