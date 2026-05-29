package dpp.repo.client.core;

/**
 * Converts a caller-owned DPP type to and from full JSON payloads used by repository endpoints.
 */
public interface DppCodec<T> {
    String toJson(T dpp);

    T fromJson(String json);
}
