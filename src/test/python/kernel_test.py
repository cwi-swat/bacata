import unittest
import jupyter_kernel_test

class MyKernelTests(jupyter_kernel_test.KernelTests):
    # Required --------------------------------------

    # The name identifying an installed kernel to run the tests against
    kernel_name = "javakernel"

    # language_info.name in a kernel_info_reply should match this
    language_name = "java"

    # Optional --------------------------------------

    # Code in the kernel's language to write "hello, world" to stdout
    code_hello_world = "1+1;"

    # Pager: code that should display something (anything) in the pager
    # code_page_something = "1+1;"

    # Samples of code which generate a result value (ie, some text
    # displayed as Out[n])
    code_execute_result = [
        {'code': '6*7;', 'result': 'int: 42'}
    ]

    # Samples of code which should generate a rich display output, and
    # the expected MIME type
    code_display_data = [
        {'code': '1+1;', 'mime': 'text/plain'}
    ]

    # You can also write extra tests. We recommend putting your kernel name
    # in the method name, to avoid clashing with any tests that
    # jupyter_kernel_test adds in the future.
    # def test_mykernel_stderr(self):
    #     reply, output_msgs = self.execute_helper(code='print_err "oops"')
    #     self.assertEqual(output_msgs[0]['msg_type'], 'stream')
    #     self.assertEqual(output_msgs[0]['content']['name'], 'stderr')
    #     self.assertEqual(output_msgs[0]['content']['text'], 'oops\n')

if __name__ == '__main__':
    unittest.main()
