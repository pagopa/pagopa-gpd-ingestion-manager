import {createClient} from "redis";

const redisHost = "127.0.0.1";
const redisPort = "6379";

const client = createClient({
    socket: {
        port: redisPort,
        host: redisHost
    }
});

client.on('error', err => console.log('Redis Client Error', err))
client.connect();

client.on('connect', function () {
    console.log('Connected!');
});

async function readFromRedisWithKey(key) {
    return await client.get(key);
}

async function setValueRedis({key, value}){
    return await client.set(key, value);
}

async function shutDownClient() {
    await client.quit();
  }

module.exports = {
    readFromRedisWithKey, shutDownClient, setValueRedis
  }