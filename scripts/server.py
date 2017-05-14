import os
from flask import Flask, send_from_directory

app = Flask(__name__)

PATH_PREFIX='/home/vivek/Work/dobby-android/Dobby/app/build/spoon/wifidoc/debug/'

@app.route('/')
def index_file():
    return send_from_directory(PATH_PREFIX, 'index.html')

@app.route('/<path:filename>')
def serve_static(filename):
    return send_from_directory(PATH_PREFIX, filename)


if __name__ == '__main__':
    port = int(os.getenv('PORT', 5187))
    print ("Starting app on port %d" % port)
    app.run(debug=True, port=port, host='0.0.0.0')

