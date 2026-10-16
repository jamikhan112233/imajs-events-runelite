package com.imajsevents;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Singleton
class ImajEventsServiceClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	private final OkHttpClient httpClient;
	private final Gson gson;

	@Inject
	ImajEventsServiceClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
	}

	void enroll(String baseUrl, String competition, char[] password, String rsn,
		Consumer<EnrollmentResponse> success, Consumer<String> failure)
	{
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("competition", competition.trim());
		payload.put("password", new String(password));
		payload.put("rsn", rsn.trim());
		post(baseUrl, "/api/v1/enroll", null, payload, EnrollmentResponse.class, success, failure);
		java.util.Arrays.fill(password, '\0');
	}

	void refreshProfile(String baseUrl, String token, Consumer<CompetitionProfile> success, Consumer<String> failure)
	{
		request(baseUrl, "/api/v1/profile", token, "GET", null, CompetitionProfile.class, success, failure);
	}

	void changeRsn(String baseUrl, String token, String newRsn, String code,
		Consumer<CompetitionProfile> success, Consumer<String> failure)
	{
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("newRsn", newRsn.trim());
		payload.put("code", code.trim());
		post(baseUrl, "/api/v1/rsn-change", token, payload, CompetitionProfile.class, success, failure);
	}

	void sendEvent(String baseUrl, String token, Map<String, Object> payload)
	{
		post(baseUrl, "/api/v1/events", token, payload, EmptyResponse.class,
			ignored -> log.debug("Imaj's Events notification accepted"),
			error -> log.debug("Imaj's Events notification failed: {}", error));
	}

	void sendEvent(String baseUrl, String token, Map<String, Object> payload, byte[] screenshot)
	{
		if (screenshot == null || screenshot.length == 0)
		{
			sendEvent(baseUrl, token, payload);
			return;
		}
		final String url;
		try
		{
			url = ServiceUrls.endpoint(baseUrl, "/api/v1/events");
		}
		catch (IllegalArgumentException ex)
		{
			log.warn("Imaj's Events screenshot notification failed: {}", ex.getMessage());
			return;
		}

		RequestBody body = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("payload_json", gson.toJson(payload))
			.addFormDataPart("file", "completion.jpg",
				RequestBody.create(MediaType.parse("image/jpeg"), screenshot))
			.build();
		Request request = new Request.Builder()
			.url(url)
			.header("Accept", "application/json")
			.header("Authorization", "Bearer " + token)
			.post(body)
			.build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Imaj's Events screenshot notification could not reach the service", e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response closeable = response)
				{
					String responseBody = closeable.body() == null ? "" : closeable.body().string();
					if (!closeable.isSuccessful())
					{
						log.warn("Imaj's Events screenshot notification rejected ({}): {}",
							closeable.code(), responseBody);
						return;
					}
					log.debug("Imaj's Events screenshot notification accepted");
				}
				catch (IOException ex)
				{
					log.warn("Unable to read the Imaj's Events screenshot response", ex);
				}
			}
		});
	}

	private <T> void post(String baseUrl, String path, String token, Object payload, Class<T> responseType,
		Consumer<T> success, Consumer<String> failure)
	{
		request(baseUrl, path, token, "POST", gson.toJson(payload), responseType, success, failure);
	}

	private <T> void request(String baseUrl, String path, String token, String method, String json,
		Class<T> responseType, Consumer<T> success, Consumer<String> failure)
	{
		final String url;
		try
		{
			url = ServiceUrls.endpoint(baseUrl, path);
		}
		catch (IllegalArgumentException ex)
		{
			failure.accept(ex.getMessage());
			return;
		}

		Request.Builder builder = new Request.Builder().url(url).header("Accept", "application/json");
		if (token != null && !token.isEmpty())
		{
			builder.header("Authorization", "Bearer " + token);
		}
		if ("GET".equals(method))
		{
			builder.get();
		}
		else
		{
			builder.post(RequestBody.create(JSON, json));
		}

		httpClient.newCall(builder.build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				failure.accept("Could not reach the event service");
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response closeable = response)
				{
					String body = closeable.body() == null ? "" : closeable.body().string();
					if (!closeable.isSuccessful())
					{
						ErrorResponse error = body.isEmpty() ? null : gson.fromJson(body, ErrorResponse.class);
						failure.accept(error != null && error.error != null ? error.error : "Request rejected (" + closeable.code() + ")");
						return;
					}
					if (body.isEmpty() && responseType != EmptyResponse.class)
					{
						failure.accept("The event service returned an empty response");
						return;
					}
					T value = body.isEmpty() ? responseType.cast(new EmptyResponse()) : gson.fromJson(body, responseType);
					success.accept(value);
				}
				catch (Exception ex)
				{
					log.debug("Unable to parse Imaj's Events response", ex);
					failure.accept("The event service returned an invalid response");
				}
			}
		});
	}

	@Data
	static class EnrollmentResponse
	{
		private String enrollmentToken;
		private CompetitionProfile profile;
	}

	private static class ErrorResponse
	{
		private String error;
	}

	private static class EmptyResponse
	{
	}
}
