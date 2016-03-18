import json

messages = {}

def main():
	i = 0;
	try:
		while True:
			with open("../bin/ses_process" + str(i) + ".log", 'r') as f:
				for line in f:
					read_log(line, i)
			i += 1
	except IOError:
		pass

	test_causal_order()

def read_log(line, i):
	if line.startswith("Delivered"):
		message_id = line[19:22]
		sent_timestamp_start = line.index("[")
		sent_timestamp_end = line.index("]")
		sent_timestamp = line[sent_timestamp_start : sent_timestamp_end+1]
		rec_timestamp_start = line.rindex("[")
		rec_timestamp = line[rec_timestamp_start:]
		messages[message_id] = [json.loads(sent_timestamp), json.loads(rec_timestamp)]

def test_causal_order():
	for message in messages:
		for other in messages:
			if compare_vectors(message[0], other[0]):
				assert compare_vectors(message[1], message[0])

def compare_vectors(vector1, vector2):
	for i in range(len(vector1)):
		if vector1[i] > vector2[i]:
			return False
	return True

print "Starting..."
main()
print "...Done!"
print "Causal order verified for messages:"
for message in messages:
	print message, messages[message]