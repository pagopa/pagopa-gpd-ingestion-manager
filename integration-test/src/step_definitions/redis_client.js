const { createClient } = require('redis');

const client = createClient({
    socket: {
        port: "6379",
        host: "127.0.0.1"
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

module.exports = {
    readFromRedisWithKey
  }