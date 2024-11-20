export const VALID_CF = "PRFGPD24S20B157N";
export const INVALID_CF = "invalidCF";
export const entityIdentifier = "PERFORMANCE_TEST_GPD_INGESTION";
export const REDIS_ARRAY_IDS_TOKENIZED = "redisTokenized";
export const REDIS_ARRAY_IDS_NOT_TOKENIZED = "redisNotTokenized";

export function randomString(length, charset) {
    let res = '';
    while (length--) res += charset[(Math.random() * charset.length) | 0];
    return res;
}
