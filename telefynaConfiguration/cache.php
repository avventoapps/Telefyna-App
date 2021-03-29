<?php
    $now = date('Y-m-d_H:i:s');
    file_put_contents("exports/".$now.".txt", json_encode($_POST, JSON_PRETTY_PRINT)."\r\n______________________________________________________\r\n");
?>