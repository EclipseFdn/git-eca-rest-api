#!/usr/bin/env ruby

require 'json'
require 'httparty'
require 'multi_json'

## Read in the arguments passed from GitLab and split them to an arg array
stdin_raw = ARGF.read;
stdin_args = stdin_raw.split(/\s+/)

## Set the vars for the commit hashes of current pre-receive event
previous_head_commit = stdin_args[0]
new_head_commit = stdin_args[1]

## Create either a range of commits for existing refs, or the HEAD commit SHA for new refs
commit_range_declaration = ''
if (previous_head_commit.match(/^0+$/)) then
  commit_range_declaration = new_head_commit
else
  commit_range_declaration = "#{previous_head_commit}..#{new_head_commit}"
end

## Retrieve the list of commits for the given range
git_commit_data = `git rev-list --first-parent #{commit_range_declaration}`
## Create a list of commits to iterate over from the command line response
git_commits = git_commit_data.split(/\s+/)

## Process each of the commits, creating a line of formatted JSON data to POST
processed_git_data = []
git_commits.each do |commit|
  ## Get parents separately to post-process string into array format
  commit_parents_raw = `git show -s --format='%P' #{commit}`
  commit_parents = commit_parents_raw.split(/\s/)
  ## Process Git data into JSON for each commit found
  git_data = `git show -s --format='{"author": {"name":"%an","mail":"%ae"},"committer":{"name":"%cn","mail":"%ce"},"body":"%B","subject":"%s","hash":"%H", "parents":["#{commit_parents.join("\", \"")}"]}' #{commit}`
  ## Strip new lines as they FUBAR JSON parsers
  git_data = git_data.gsub(/[\n\r]/, ' ')
  processed_git_data.push(MultiJson.load(git_data))
end

## Get the project ID from env var, extracting from pattern 'project-###'
project_id = ENV['GL_REPOSITORY'][8..-1]
## Get data about project from API
project_response = HTTParty.get("https://gitlab.eclipse.org/api/v4/projects/#{project_id}", 
  :headers => {
    'Authorization' => 'Bearer '+ENV['GL_ACCESS_TOKEN']
  })
## Format data to be able to easily read and process it
project_json_data = MultiJson.load(project_response.body)
## Get the web URL, checking if project is a fork to get original project URL
if (!project_json_data['forked_from_project'].nil? && !project_json_data['forked_from_project']['web_url'].nil?)
  project_url = project_json_data['forked_from_project']['web_url']
else 
  project_url = project_json_data['web_url']
end

## Create the JSON payload
json_data = {
  :repoUrl => project_url,
  :provider => 'gitlab',
  :commits => processed_git_data
}
## Generate request
response = HTTParty.post("https://api.eclipse.org/git/eca", :body => MultiJson.dump(json_data), 
  :headers => { 
    'Content-Type' => 'application/json'
  })
## convert request to hash map
parsed_response = MultiJson.load(response.body)

## for each discovered hash commit tracked by response, report if it was OK
commit_keys = parsed_response['commits'].keys
commit_keys.each do |key|
  commit_status = parsed_response['commits'][key]
  if (commit_status['errors'].empty?) then
    puts "Commit: #{key}\t\t✔\n\n"
    commit_status['messages'].each do |msg|
      puts "\t#{msg['message']}"
    end
    if (commit_status['warnings'].empty?) then
      commit_status['messages'].each do |msg|
        puts "\t#{msg['message']}"
      end
      puts "Any warnings noted above may indicate compliance issues with committer ECA requirements. More information may be found on https://www.eclipse.org/legal/ECA.php"
    end
    puts "\n\n"
  else
    puts "Commit: #{key}\t\tX\n\n"
    commit_status['messages'].each do |msg|
      puts "\t#{msg['message']}"
    end
    puts "\n"
    commit_status['errors'].each do |error|
      puts "GL-HOOK-ERR: #{error['message']}"
    end
    puts "\n\n"
  end
end
## If error, exit as status 1
if (response.code == 403) then
  exit 1
end
