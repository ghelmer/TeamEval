#!/usr/bin/perl

use Data::Dumper;
use MIME::Lite;
use strict;

my $mailhost = 'mailin.iastate.edu';

my $student_team_fn = '/Users/ghelmer/Documents/Classes/MIS307S15/Homeworks/Team Evaluations/students-teams.txt';

my %team_by_id;
my %teams;
my %recip;

open(IN, "< $student_team_fn") || die "Could not open ${student_team_fn}: $!";
while (<IN>) {
    chomp;
    next if /^#/;
    my ($id, $team) = split;
    $team_by_id{$id} = $team;
    if (!defined($teams{$team})) {
	$teams{$team} = [];
    }
    push(@{$teams{$team}}, $id);
    $recip{$id} = 0;
}
close(IN);

foreach my $key (sort {$a <=> $b} keys(%teams)) {
    print "Team $key: ", Dumper($teams{$key}), "\n";
}

exit 0;

MIME::Lite->send('smtp', $mailhost, Timeout=>60);

my $done = 0;
while (!$done) {
    my $retry = 0;
    foreach my $key (sort {$a <=> $b} keys(%teams)) {
	foreach my $i (@{$teams{$key}}) {
	    next if $recip{$i};
	    #print "cp TeamEval-${key}.xlsx TeamEval-${key}-${i}.xlsx\n";
	    #system("cp TeamEval-${key}.xlsx TeamEval-${key}-${i}.xlsx");
	    my $msg = MIME::Lite->new(
		From    => "ghelmer\@iastate.edu",
		To      => "${i}\@iastate.edu",
		CC      => "ghelmer\@iastate.edu",
		Subject => "MIS307 Fall 2014 Final Project Team Evaluation for Team ${key} Member ${i}",
		Type    =>'multipart/related'
		);
	    $msg->attach(
		Type => 'text/html',
		Data => "<body>\n" .
		"Please find attached the Excel spreadsheet for MIS307 Team ${key}.\n" .
		"Please fill in your evaluation for each team member (including yourself)\n" .
		"and email it back to ghelmer\@iastate.edu by\n" .
		"<strong>11:59pm Friday, December 12.</strong>\n" .
		"<p/>\n" .
		"<strong>Please do not change the name of the spreadsheet file, and if you\n" .
		"edit it in Google Docs, please send the actual spreadsheet back to me\n" .
		"(not just a link).</strong>\n" .
		"<p/></body>\n",
		);
	    $msg->attach(
		Type => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
		Id   => "TeamEval-${key}-${i}.xlsx",
		Filename=> "TeamEval-${key}-${i}.xlsx",
		Path => "/Users/ghelmer/Documents/Classes/MIS307F14/Homeworks/Team Evaluations/Sent/TeamEval-${key}.xlsx",
		);
	    my $sent = 0;
	    # Catch unexpected termination during send.
	    eval { $msg->send; };
	    if ($@) {
		print "Sending message to ${i} failed.\n";
	    } else {
		$sent = $msg->last_send_successful();
	    }
	    if (!$sent) {
		print "Sending message to ${i} was unsuccessful.\n";
		$retry++;
	    } else {
		print "Sent message to ${i}.\n";
		$recip{$i} = 1;
	    }
	}
    }
    if ($retry == 0) {
	$done = 1;
    } else {
	print "${retry} receipients left. Waiting one minute.\n";
	sleep(60);
    }
}
