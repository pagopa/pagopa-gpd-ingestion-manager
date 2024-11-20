FROM node:16

WORKDIR /src/node-app

COPY ./ ./

RUN yarn install

ENTRYPOINT ["yarn","start"]