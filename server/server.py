import os
from flask import Flask, send_from_directory, make_response
from functools import update_wrapper
app = Flask(__name__)

PATH_PREFIX='/home/vivek/Work/dobby-android/Dobby/app/build/spoon/wifidoc/debug/'

def nocache(f):
    def new_func(*args, **kwargs):
        resp = make_response(f(*args, **kwargs))
        resp.cache_control.no_cache = True
        return resp
    return update_wrapper(new_func, f)

@app.route('/')
@nocache
def index_file():
    return send_from_directory(PATH_PREFIX, 'index.html')

@app.route('/<path:filename>')
@nocache
def serve_static(filename):
	print ("Returning {0} from {1}".format(filename, PATH_PREFIX + filename))
	return send_from_directory(PATH_PREFIX, filename)


if __name__ == '__main__':
    port = int(os.getenv('PORT', 5187))
    print ("Starting app on port %d" % port)
    app.run(debug=True, port=port, host='0.0.0.0')

