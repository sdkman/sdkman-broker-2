# Corrective actions

I have generated a one-shot feature in the application using a single prompt. I have reviewed the feature and found some issues. I have marked each issue with a TODO comment.

I would like you to go through this repository's source code and read all the TODO comments that I have added. Group these comments into logical tasks that we can tackle as small units of work. Sometimes these tasks could span multiple TODO comments across different files. At this point you've only done a grouping _without_ writing anything to a file.

Then I want you to generate a new document called `TODO.md` in the root directory of this project. This file should contain the list of logical tasks that you compiled in the previous step. Each entry in this file should contain the following detail:

* A checkbox to indicate if the task is done or not
* The headline of the task
* A description of what the task entails

## IMPORTANT!

* Each entry in `TODO.md` should be structured as a prompt that we can use to generate the code to complete that task. The prompt must have all the relevant detail and context for an AI agent to complete the entire task in one shot.
* Each prompt should be followed by the list of files that are affected.
* Each task should be scoped correctly so it will be completed in a single shot.
* Do not make up your own TODOs! Only use the ones provided in the source code!
