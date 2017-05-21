import os
from flask import Flask, send_from_directory, make_response, render_template
from functools import update_wrapper
app = Flask(__name__)

#PATH_PREFIX='/home/vivek/Work/dobby-android/Dobby/app/build/spoon/wifidoc/debug/'
PATH_PREFIX='/home/vivek/Work/dobby-android/server/spoon/'

def nocache(f):
    def new_func(*args, **kwargs):
        resp = make_response(f(*args, **kwargs))
        resp.cache_control.no_cache = True
        return resp
    return update_wrapper(new_func, f)

def generate_link_map(input_path):
	link_map = {}
	list_of_flavors = [name for name in os.listdir(input_path) if os.path.isdir(os.path.join(input_path, name))]
	for flavor in list_of_flavors:
		sub_dir_path = os.path.join(input_path, flavor)
		list_of_apis = [name for name in os.listdir(sub_dir_path) if os.path.isdir(os.path.join(sub_dir_path, name))]
		for api in list_of_apis:
			link_key = flavor + api
			link_map[link_key] = os.path.join(flavor, api, 'index.html')
	return link_map
	

@app.route('/')
@nocache
def index_file():
	link_map = generate_link_map(PATH_PREFIX)
	print ("Link map is {0}".format(link_map))
	return render_template('main.html', link_map=link_map)

@app.route('/<string:flavor>/<int:api>/<path:filename>')
@nocache
def serve_static(flavor, api, filename):
	print ("Returning {0} from {1}".format(filename, PATH_PREFIX + filename))
	input_dir_to_use = os.path.join(PATH_PREFIX, flavor, str(api))	
	return send_from_directory(input_dir_to_use, filename)


if __name__ == '__main__':
    port = int(os.getenv('PORT', 5187))
    print ("Starting app on port %d" % port)
    app.run(debug=True, port=port, host='0.0.0.0')

