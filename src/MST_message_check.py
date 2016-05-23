import re

s_init = 0
s_test = 0
s_rej  = 0
s_acc  = 0
s_rep  = 0
s_chr  = 0
s_con  = 0
s_halt = 0

r_init = 0
r_test = 0
r_rej  = 0
r_acc  = 0
r_rep  = 0
r_chr  = 0
r_con  = 0
r_halt = 0

merge  = 0
absorb = 0

frag   = -1
level  = -1

def main():
	i = 0;
	try:
		while True:
			with open("../bin/mst_process" + str(i) + ".log", 'r') as f:
				for line in f:
					read_log(line, i)
			i += 1
	except IOError:
		pass

	check_message_count()

def read_log(line, i):
	if   line.startswith("Sending Initiate"):
		global s_init
		s_init += 1
	elif line.startswith("Sending Test"):
		global s_test
		s_test += 1
	elif line.startswith("Sending Reject"):
		global s_rej
		s_rej  += 1
	elif line.startswith("Sending Accept"):
		global s_acc
		s_acc  += 1
	elif line.startswith("Sending Report"):
		global s_rep
		s_rep  += 1
	elif line.startswith("Sending ChangeRoot"):
		global s_chr
		s_chr  += 1
	elif line.startswith("Sending Connect"):
		global s_con
		s_con  += 1
	elif line.startswith("Sending Halt"):
		global s_halt
		s_halt += 1
	elif line.startswith("Received Initiate"):
		global r_init
		r_init += 1
	elif line.startswith("Received Test"):
		global r_test
		r_test += 1
	elif line.startswith("Received Reject"):
		global r_rej
		r_rej  += 1
	elif line.startswith("Received Accept"):
		global r_acc
		r_acc  += 1
	elif line.startswith("Received Report"):
		global r_rep
		r_rep  += 1
	elif line.startswith("Received ChangeRoot"):
		global r_chr
		r_chr  += 1
	elif line.startswith("Received Connect"):
		global r_con
		r_con  += 1
	elif line.startswith("Received Halt"):
		global r_halt
		r_halt += 1
	elif line.startswith("Merging fragments"):
		global merge
		merge  += 1
	elif line.startswith("Absolving fragment"):
		global absorb
		absorb += 1
	elif line.startswith("Final fragment:"):
		global frag
		global level
		info = [int(s) for s in re.findall(r'(\d+)', line)]
		if (level >= 0):
			assert info[0] == frag
			assert info[1] == level
		else:
			frag  = info[0]
			level = info[1]

def check_message_count():
	global s_init
	global s_test
	global s_rej
	global s_acc
	global s_rep
	global s_chr
	global s_con
	global s_halt
	global r_init
	global r_test
	global r_rej
	global r_acc
	global r_rep
	global r_chr
	global r_con
	global r_halt
	global merge
	assert s_init == r_init
	assert s_test == r_test
	assert s_rej  == r_rej
	assert s_acc  == r_acc
	assert s_rep  == r_rep
	assert s_chr  == r_chr
	assert s_con  == r_con
	assert s_halt == r_halt
	assert merge % 2 == 0
	merge //= 2

main()
print(s_init)
print(s_test)
print(s_rej)
print(s_acc)
print(s_rep)
print(s_chr)
print(s_con)
print(s_halt)
print(s_init+s_test+s_rej+s_acc+s_rep+s_chr+s_con+s_halt)
print(merge)
print(absorb)
print(merge+absorb)
print(frag)
print(level)
