import json
import os

from dotenv import load_dotenv
from flask import Flask, jsonify
import pika
import requests

app = Flask(__name__)
load_dotenv()

RABBITMQ_USER = os.getenv("RABBITMQ_USER")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD")
API_GATEWAY_URL = os.getenv("AWS_API_GATEWAY")
API_GATEWAY_KEY = os.getenv("AWS_API_GATEWAY_KEY")
RABBIT_PORT = os.getenv("RABBIT_PORT")


def main():
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASSWORD)
    conn = pika.BlockingConnection(pika.ConnectionParameters('rabbitmq-bank-api', RABBIT_PORT, '/', credentials))
    channel = conn.channel()
    channel.queue_declare(queue='email.queue')

    def callback(ch, method, properties, body):
        decoded_body = body.decode('utf-8')
        reply_to = properties.reply_to
        correlation_id = properties.correlation_id

        try:
            data = send_data(decoded_body)
            print("Data: ", data)

            status_code = data.get('status_code')

            if status_code not in [200, 201]:
                raise Exception(data.get("error"))

            data_body = data.get('message')

            response_body = json.dumps({
                "success": True,
                "statusCode": status_code,
                "data": data_body,
            })
        except Exception as e:
            response_body = json.dumps({
                "success": False,
                "error": str(e)
            })

            print("Error body: ", response_body)

        print("Response body: ", response_body)

        if reply_to and correlation_id:
            ch.basic_publish(
                exchange='',
                routing_key=reply_to,
                properties=pika.BasicProperties(
                    correlation_id=correlation_id,
                    content_type='application/json'
                ),
                body=response_body.encode('utf-8')
            )
            print(f"Response sent to {reply_to} with correlation_id: {correlation_id}")

        ch.basic_ack(delivery_tag=method.delivery_tag)

    channel.basic_consume(queue='email.queue', auto_ack=False, on_message_callback=callback)
    channel.start_consuming()


def send_data(decoded_body):
    headers = {
        'Content-Type': 'application/json',
        "x-api-key": API_GATEWAY_KEY,
    }
    url = API_GATEWAY_URL + "/process-data-to-ses"

    response = requests.post(url, headers=headers, json=decoded_body)
    result = response.json()
    result["status_code"] = response.status_code

    return result


if __name__ == '__main__':
    main()
