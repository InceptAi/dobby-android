"""Helper functions.
"""
import sys
import os
from optparse import OptionParser
__author__ = """\n""".join(['Vivek Shrivastava (vivek@obiai.tech)'])

def get_float_value(json_dict, key):
    if not json_dict or not key:
        return None
    try:
        value = float(json_dict[key]) if json_dict.get(key, None) else None
    except ValueError:
        print ("Cannot convert to float", json_dict.get(key, None))
        return None
    else:
        return value

def read_json(file_stream):
    json_to_return = None
    try:
        json_to_return = json.load(file_stream)
    except json.decoder.JSONDecodeError as e:
        print ("Invalid JSON input ({0}). Input:{1}".format(e.msg, e.doc))
        return None
    else:
        return json_to_return

def read_json_from_file(file_name):
    json_to_return = None
    try:
        with open(file_name) as file_stream:
            json_to_return = read_json(file_stream)
    except (OSError, IOError) as e:
        print ("Cannot read file {file_to_read}.".format(file_to_read=file_name))
        return None
    else:
        return json_to_return

def compute_channel_contention_with_gap(gap):
    SNR_GAP_FOR_MAX_CONTENTION = 10.0
    SNR_MAX_POSITIVE_GAP_SQ = SNR_GAP_FOR_MAX_CONTENTION * SNR_GAP_FOR_MAX_CONTENTION
    DEPRECIATION_FACTOR_FOR_1_OVER_SNR_GAP = 0.7
    BASE_CONTENTION_LEVEL = 0
    EPSILON = 1.0
    contention = 0.0
    if abs(gap) < 5:
        contention = 0.2
    if gap > 0:
        contention += 1 / pow((gap) + EPSILON, DEPRECIATION_FACTOR_FOR_1_OVER_SNR_GAP)
    else:
        contention += abs(gap) / SNR_GAP_FOR_MAX_CONTENTION
    return contention



def compute_overall_contention(own_ap_signal, other_ap_signal_list):
    contention = 0.0
    for other_ap_signal in other_ap_signal_list:
        gap = own_ap_signal - other_ap_signal
        contention += compute_channel_contention_with_gap(gap)
    return contention

def read_signals_file(signals_file):
    signals = []
    if not os.path.isfile(signals_file):
        sys.stderr.write("signals file not found at %s\n" % opts.signals_file)
        sys.exit(-1)
    try:
        with open(signals_file, 'r') as f:
            data=f.readlines()
        signals = [int(x.strip()) for x in data]
    except (OSError, IOError) as e:
        print ("Cannot read file {file_to_read}.".format(file_to_read=opts.signals_file))
    except ValueError as e:
        print ("Non integer values in {file_to_read}.".format(file_to_read=opts.signals_file))
    return signals

def main():
    op = OptionParser()
    op.add_option("-v", "--verbose", action="store_true", help="verbose", default=False)
    op.add_option("-f", "--signalsfile", dest="signals_file", help="signal of aps to compute contention against (in dbm)", default=None)
    op.add_option("-s", "--snr", dest="own_signal", help="Own signal strength in dbm", default="-80")
    (opts, args) = op.parse_args()
    if not opts.signals_file:   # if filename is not given
        op.error('Signals file not given')

    signals = read_signals_file(opts.signals_file)

    try:
        own_signal = int(opts.own_signal)
    except ValueError as e:
        print ("Own signal must be int{wrong_signal}".format(wrong_signal=opts.own_signal))
    contention = compute_overall_contention(own_signal, signals)
    print ("Contention:{0}".format(contention))

if __name__ == '__main__':
    main()


